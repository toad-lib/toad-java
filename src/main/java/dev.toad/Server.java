package dev.toad;

import dev.toad.Async;
import dev.toad.msg.Code;
import dev.toad.msg.Message;
import dev.toad.msg.Payload;
import dev.toad.msg.Type;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Server implements AutoCloseable {

  boolean closed = false;
  Optional<Async.LoopHandle> loopHandle = Optional.empty();
  final Toad toad;
  final ArrayList<Middleware> middlewares;

  Server(Toad toad, ArrayList<Middleware> ms) {
    this.toad = toad;
    this.middlewares = ms;
  }

  public DatagramChannel channel() {
    return this.toad.channel();
  }

  dev.toad.msg.ref.Message blockUntilRequest()
    throws InterruptedException, ExecutionException {
    return Async
      .pollCompletable(() -> {
        if (this.closed) {
          throw new Exit();
        }

        return this.toad.pollReq();
      })
      .get();
  }

  void tick() {
    try {
      var req = this.blockUntilRequest();
      var addr = req.addr().get();

      this.toad.logger().log(Level.FINE, Toad.LogMessage.rx(req));

      var result = Middleware.Result.next();
      var acked = new AtomicBoolean(false);
      for (var m : this.middlewares) {
        var ctx = new Middleware.Context(this.toad.logger(), result, req);
        result =
          Middleware.tick(
            m,
            ctx,
            () -> {
              if (req.type() != Type.CON) {
                return;
              }

              acked.set(true);
              this.toad.logger().log(Level.FINE, LogMessage.earlyAck(req));
              var ack = req
                .buildResponse()
                .code(Code.EMPTY)
                .type(Type.ACK)
                .build();
              this.toad.logger().log(Level.FINE, Toad.LogMessage.tx(ack));
              try {
                Async.pollCompletable(() -> this.toad.sendMessage(ack)).get();
              } catch (Throwable t) {
                this.toad.logger().log(Level.SEVERE, Toad.LogMessage.tx(ack));
              }
            }
          );
      }

      var resp = result.response().get();
      if (resp.isEmpty()) {
        this.toad.logger().log(Level.SEVERE, LogMessage.unhandledRequest(req));
      } else {
        var resp_ = resp.get();
        var resp__ = resp_.type() == Type.ACK && acked.get()
          ? resp_.buildCopy().type(Type.CON).build()
          : resp_;
        this.toad.logger().log(Level.FINE, Toad.LogMessage.tx(resp__));
        Async.pollCompletable(() -> this.toad.sendMessage(resp__)).get();
      }

      req.close();
    } catch (Throwable e) {
      if (e.getCause() instanceof Exit || e instanceof Exit) {
        return;
      }

      this.toad.logger().log(Level.SEVERE, e.toString());
      e.printStackTrace();
    }
  }

  public void notify(String path) {
    this.toad.notify(path);
  }

  public Async.LoopHandle run() {
    if (!this.loopHandle.isEmpty()) {
      return this.loopHandle.get();
    }

    try {
      this.toad.logger()
        .log(Level.INFO, LogMessage.startup(this.toad.localAddress()));
    } catch (Throwable t) {}

    this.loopHandle = Optional.of(Async.loop(() -> this.tick()));
    return this.loopHandle.get();
  }

  @Override
  public void close() {
    this.closed = true;

    try {
      if (!this.loopHandle.isEmpty()) {
        this.loopHandle.get().stop();
      }
    } catch (Throwable e) {}

    this.toad.close();
  }

  static final class LogMessage {

    static String startup(InetSocketAddress addr) {
      return String.format("Server listening on %s", addr.toString());
    }

    static String earlyAck(Message req) {
      return String.format("Separately ACKing request %s", req.toDebugString());
    }

    static String unhandledRequest(Message req) {
      return String.format(
        "Server never generated response for message\n%s",
        req
      );
    }

    static String ex(Message m) {
      return String.format(
        "Exception thrown while handling:\n%s\nException:",
        m.toDebugString()
      );
    }
  }

  static final class Exit extends RuntimeException {}

  public static final class Middleware {

    final BiPredicate<Result, Message> shouldRun;
    final Function<Context, Result> fun;

    static final CompletableFuture<Optional<Message>> noop =
      CompletableFuture.completedFuture(Optional.empty());

    public static final Middleware respondNotFound =
      Middleware.requestHandler(m ->
        Optional.of(m.buildResponse().code(Code.NOT_FOUND).build())
      );

    public static final Middleware handlePing = Middleware.requestHandler(m ->
      Type.eq.test(m.type(), Type.CON) && Code.eq.test(m.code(), Code.EMPTY)
        ? Optional.of(
          m.buildResponse().code(Code.EMPTY).type(Type.RESET).build()
        )
        : Optional.empty()
    );

    public static final Middleware handleException = new Middleware(
      (r, _m) -> r instanceof ResultError,
      ctx -> {
        var e = ((ResultError) ctx.result).error;

        ctx.logger.log(Level.SEVERE, LogMessage.ex(ctx.request), e);

        return Middleware.Result.respond(
          ctx.request.buildResponse().code(Code.INTERNAL_SERVER_ERROR).build()
        );
      }
    );

    public static final Middleware handleExceptionDebug = new Middleware(
      (r, _m) -> r instanceof ResultError,
      ctx -> {
        var e = ((ResultError) ctx.result).error;

        ctx.logger.log(Level.SEVERE, LogMessage.ex(ctx.request), e);

        return Middleware.Result.respond(
          ctx.request
            .buildResponse()
            .code(Code.INTERNAL_SERVER_ERROR)
            .payload(Payload.text(e.toString()))
            .build()
        );
      }
    );

    static Result tick(Middleware m, Context ctx, Runnable separatelyAck) {
      if (!m.shouldRun.test(ctx.result, ctx.request)) {
        return ctx.result;
      }

      try {
        if (ctx.result.isAsync()) {
          ctx.result.response().get();
          separatelyAck.run();
        }

        return m.run(ctx);
      } catch (Throwable e) {
        return Middleware.Result.error(e);
      }
    }

    public static Middleware requestHandler(
      Function<Message, Optional<Message>> f
    ) {
      return new Middleware(
        (r, _m) -> !r.isFinal(),
        ctx ->
          f
            .apply(ctx.request)
            .map(Middleware.Result::respond)
            .orElse(Middleware.Result.next())
      );
    }

    public static Middleware requestHandlerAsync(
      Function<Message, Optional<CompletableFuture<Message>>> f
    ) {
      return new Middleware(
        (r, _m) -> !r.isFinal(),
        ctx ->
          f
            .apply(ctx.request)
            .map(Middleware.Result::respond)
            .orElse(Middleware.Result.next())
      );
    }

    public static Middleware responseConsumer(Consumer<Message> f) {
      return new Middleware(
        (r, _m) ->
          r instanceof ResultRespondSync || r instanceof ResultRespondAsync,
        ctx -> {
          try {
            var rep = ctx.result.response().get().get();
            f.accept(rep);
            return ctx.result;
          } catch (Throwable e) {
            throw new RuntimeException(e);
          }
        }
      );
    }

    public static Middleware exceptionHandler(Function<Throwable, Result> f) {
      return new Middleware(
        (r, _m) -> r instanceof ResultError,
        ctx -> f.apply(((ResultError) ctx.result).error)
      );
    }

    public Middleware(
      BiPredicate<Result, Message> shouldRun,
      Function<Context, Result> fun
    ) {
      this.fun = fun;
      this.shouldRun = shouldRun;
    }

    public Result run(Context context) {
      return this.fun.apply(context);
    }

    public Middleware filter(BiPredicate<Result, Message> f) {
      return new Middleware(
        (r, m) -> f.test(r, m) && this.shouldRun.test(r, m),
        this.fun
      );
    }

    public static final class Context {

      public final Logger logger;
      public final Result result;
      public final Message request;

      public Context(Logger logger, Result result, Message request) {
        this.logger = logger;
        this.result = result;
        this.request = request;
      }
    }

    public static sealed interface Result
      permits
        ResultError,
        ResultNextSync,
        ResultNextAsync,
        ResultRespondSync,
        ResultRespondAsync {
      public static Result respond(Message m) {
        return new ResultRespondSync(m);
      }

      public static Result respond(CompletableFuture<Message> m) {
        return new ResultRespondAsync(m);
      }

      public static Result error(Throwable e) {
        return new ResultError(e);
      }

      public static Result next() {
        return new ResultNextSync();
      }

      public static Result next(CompletableFuture<Void> work) {
        return new ResultNextAsync(work);
      }

      public boolean isFinal();

      public boolean isAsync();

      public CompletableFuture<Optional<Message>> response();
    }

    public static final class ResultError implements Result {

      public final Throwable error;

      public ResultError(Throwable error) {
        this.error = error;
      }

      @Override
      public boolean isFinal() {
        return true;
      }

      @Override
      public boolean isAsync() {
        return false;
      }

      @Override
      public CompletableFuture<Optional<Message>> response() {
        return Middleware.noop;
      }
    }

    public static final class ResultNextSync implements Result {

      public ResultNextSync() {}

      @Override
      public boolean isFinal() {
        return false;
      }

      @Override
      public boolean isAsync() {
        return false;
      }

      @Override
      public CompletableFuture<Optional<Message>> response() {
        return Middleware.noop;
      }
    }

    public static final class ResultNextAsync implements Result {

      final CompletableFuture<Void> work;

      public ResultNextAsync(CompletableFuture<Void> work) {
        this.work = work;
      }

      @Override
      public boolean isFinal() {
        return false;
      }

      @Override
      public boolean isAsync() {
        return true;
      }

      @Override
      public CompletableFuture<Optional<Message>> response() {
        return this.work.thenApply(_v -> Optional.empty());
      }
    }

    public static final class ResultRespondSync implements Result {

      final Message msg;

      public ResultRespondSync(Message msg) {
        this.msg = msg;
      }

      @Override
      public boolean isFinal() {
        return true;
      }

      @Override
      public boolean isAsync() {
        return false;
      }

      @Override
      public CompletableFuture<Optional<Message>> response() {
        return CompletableFuture.completedFuture(Optional.of(this.msg));
      }
    }

    public static final class ResultRespondAsync implements Result {

      final CompletableFuture<Message> msg;

      public ResultRespondAsync(CompletableFuture<Message> msg) {
        this.msg = msg;
      }

      @Override
      public boolean isFinal() {
        return true;
      }

      @Override
      public boolean isAsync() {
        return true;
      }

      @Override
      public CompletableFuture<Optional<Message>> response() {
        return this.msg.thenApply(Optional::of);
      }
    }
  }

  public static final class Builder {

    final Toad toad;
    final ArrayList<Middleware> middlewares = new ArrayList<>();

    Builder(Toad toad) {
      this.toad = toad;
    }

    public Builder middleware(Middleware m) {
      this.middlewares.add(m);
      return this;
    }

    public Builder put(String path, Function<Message, Optional<Message>> f) {
      return this.put(path, Middleware.requestHandler(f));
    }

    public Builder put(String path, Middleware m) {
      return this.middleware(
          m.filter((_r, req) ->
            Code.eq.test(req.code(), Code.PUT) &&
            req
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty())
          )
        );
    }

    public Builder post(String path, Function<Message, Optional<Message>> f) {
      return this.post(path, Middleware.requestHandler(f));
    }

    public Builder post(String path, Middleware m) {
      return this.middleware(
          m.filter((_r, req) ->
            Code.eq.test(req.code(), Code.POST) &&
            req
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty())
          )
        );
    }

    public Builder get(String path, Function<Message, Optional<Message>> f) {
      return this.get(path, Middleware.requestHandler(f));
    }

    public Builder get(String path, Middleware m) {
      return this.middleware(
          m.filter((_r, req) ->
            Code.eq.test(req.code(), Code.GET) &&
            req
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty())
          )
        );
    }

    public Builder delete(String path, Function<Message, Optional<Message>> f) {
      return this.delete(path, Middleware.requestHandler(f));
    }

    public Builder delete(String path, Middleware m) {
      return this.middleware(
          m.filter((_r, req) ->
            Code.eq.test(req.code(), Code.DELETE) &&
            req
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty())
          )
        );
    }

    public Builder onRequest(Consumer<Message> f) {
      return this.middleware(
          Middleware.requestHandler(m -> {
            f.accept(m);
            return Optional.empty();
          })
        );
    }

    public Builder onResponse(Consumer<Message> f) {
      return this.middleware(Middleware.responseConsumer(f));
    }

    public Server buildDebugExceptionHandler() {
      this.middleware(Middleware.handleExceptionDebug);
      this.middleware(Middleware.handlePing);
      this.middleware(Middleware.respondNotFound);
      return new Server(this.toad, this.middlewares);
    }

    public Server build() {
      this.middleware(Middleware.handleException);
      this.middleware(Middleware.handlePing);
      this.middleware(Middleware.respondNotFound);
      return new Server(this.toad, this.middlewares);
    }
  }
}
