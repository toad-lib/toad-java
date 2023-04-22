package dev.toad;

import dev.toad.msg.Code;
import dev.toad.msg.Message;
import dev.toad.msg.Payload;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Level;

public final class Server implements AutoCloseable {

  boolean exit = false;
  Optional<Thread> thread = Optional.empty();
  final Toad toad;
  final ArrayList<Middleware> middlewares;

  Server(Toad toad, ArrayList<Middleware> ms) {
    this.toad = toad;
    this.middlewares = ms;
  }

  public void exit() {
    this.exit = true;
  }

  public void notify(String path) {
    this.toad.notify(path);
  }

  public Thread run() {
    if (!this.thread.isEmpty()) {
      return this.thread.get();
    }

    var thread = new Thread(() -> {
      try {
        Toad
          .logger()
          .log(
            Level.INFO,
            String.format(
              "Server listening on %s",
              this.toad.localAddress().toString()
            )
          );
      } catch (Throwable t) {}

      while (true) {
        try {
          dev.toad.msg.ref.Message req = Async
            .pollCompletable(() -> {
              if (this.exit) {
                throw new Exit();
              }

              return this.toad.pollReq();
            })
            .get();
          var addr = req.addr().get();

          Toad
            .logger()
            .log(
              Level.FINE,
              String.format("<== %s\n%s", addr.toString(), req.toDebugString())
            );

          Middleware.Result result = Middleware.Result.next();
          for (var m : this.middlewares) {
            if (!m.shouldRun.test(result, req)) {
              continue;
            }

            try {
              if (result.isAsync()) {
                result.response().get();
                // Toad.ack(req);
              }

              result = m.run(result, req);
            } catch (Throwable e) {
              result = Middleware.Result.error(e);
            }
          }

          var resp = result.response().get();
          if (resp.isEmpty()) {
            Toad
              .logger()
              .log(
                Level.SEVERE,
                String.format(
                  "Server never generated response for message\n%s",
                  req.toDebugString()
                )
              );
          } else {
            var resp_ = resp.get().toOwned();
            Toad
              .logger()
              .log(
                Level.FINE,
                String.format("==> %s\n%s", addr, resp_.toDebugString())
              );
            Async.pollCompletable(() -> this.toad.sendMessage(resp_)).get();
          }

          req.close();
        } catch (Throwable e) {
          if (e.getCause() instanceof Exit || e instanceof Exit) {
            return;
          }

          Toad.logger().log(Level.SEVERE, e.toString());
          e.printStackTrace();
        }
      }
    });

    thread.start();

    this.thread = Optional.of(thread);
    return thread;
  }

  @Override
  public void close() {
    this.toad.close();
  }

  static final class Exit extends RuntimeException {}

  public static final class Middleware {

    final BiPredicate<Result, Message> shouldRun;
    final BiFunction<Result, Message, Result> fun;

    static final CompletableFuture<Optional<Message>> noop =
      CompletableFuture.completedFuture(Optional.empty());

    public static final Middleware respondNotFound =
      Middleware.requestHandler(m ->
        Optional.of(m.buildResponse().code(Code.NOT_FOUND).build())
      );

    public static final Middleware handleException = new Middleware(
      (r, _m) -> r instanceof ResultError,
      (r, m) -> {
        var e = ((ResultError) r).error;

        Toad
          .logger()
          .log(
            Level.SEVERE,
            String.format(
              "Exception thrown while handling:\n%s\nException:",
              m.toDebugString()
            ),
            e
          );

        return Middleware.Result.respond(
          m.buildResponse().code(Code.INTERNAL_SERVER_ERROR).build()
        );
      }
    );

    public static final Middleware handleExceptionDebug = new Middleware(
      (r, _m) -> r instanceof ResultError,
      (r, m) -> {
        var e = ((ResultError) r).error;

        Toad
          .logger()
          .log(
            Level.SEVERE,
            String.format(
              "Exception thrown while handling:\n%s\nException:",
              m.toDebugString()
            ),
            e
          );

        return Middleware.Result.respond(
          m
            .buildResponse()
            .code(Code.INTERNAL_SERVER_ERROR)
            .payload(Payload.text(e.toString()))
            .build()
        );
      }
    );

    public static Middleware requestHandler(
      Function<Message, Optional<Message>> f
    ) {
      return new Middleware(
        (r, _m) -> !r.isFinal(),
        (_result, request) ->
          f
            .apply(request)
            .map(Middleware.Result::respond)
            .orElse(Middleware.Result.next())
      );
    }

    public static Middleware requestHandlerAsync(
      Function<Message, Optional<CompletableFuture<Message>>> f
    ) {
      return new Middleware(
        (r, _m) -> !r.isFinal(),
        (_result, request) ->
          f
            .apply(request)
            .map(Middleware.Result::respond)
            .orElse(Middleware.Result.next())
      );
    }

    public static Middleware responseConsumer(Consumer<Message> f) {
      return new Middleware(
        (r, _m) ->
          r instanceof ResultRespondSync || r instanceof ResultRespondAsync,
        (res, request) -> {
          try {
            var rep = res.response().get().get();
            f.accept(rep);
            return res;
          } catch (Throwable e) {
            throw new RuntimeException(e);
          }
        }
      );
    }

    public static Middleware exceptionHandler(Function<Throwable, Result> f) {
      return new Middleware(
        (r, _m) -> r instanceof ResultError,
        (res, request) -> f.apply(((ResultError) res).error)
      );
    }

    public Middleware(
      BiPredicate<Result, Message> shouldRun,
      BiFunction<Result, Message, Result> fun
    ) {
      this.fun = fun;
      this.shouldRun = shouldRun;
    }

    public Result run(Result res, Message msg) {
      return this.fun.apply(res, msg);
    }

    public Middleware filter(BiPredicate<Result, Message> f) {
      return new Middleware(
        (r, m) -> f.test(r, m) && this.shouldRun.test(r, m),
        this.fun
      );
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
      this.middleware(Middleware.respondNotFound);
      return new Server(this.toad, this.middlewares);
    }

    public Server build() {
      this.middleware(Middleware.handleException);
      this.middleware(Middleware.respondNotFound);
      return new Server(this.toad, this.middlewares);
    }
  }
}
