package dev.toad;

import dev.toad.msg.Code;
import dev.toad.msg.Message;
import dev.toad.msg.Payload;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Level;

public final class Server {

  final Toad toad;
  final ArrayList<Function<Message, Middleware.Result>> middlewares;
  final Function<Message, Middleware.Result> notFoundHandler;
  final BiFunction<Message, Throwable, Middleware.Result> exceptionHandler;

  Server(
    Toad toad,
    ArrayList<Function<Message, Middleware.Result>> ms,
    Function<Message, Middleware.Result> notFoundHandler,
    BiFunction<Message, Throwable, Middleware.Result> exHandler
  ) {
    this.toad = toad;
    this.middlewares = ms;
    this.notFoundHandler = notFoundHandler;
    this.exceptionHandler = exHandler;
  }

  public void run() {
    while (true) {
      try {
        dev.toad.msg.ref.Message req = Async
          .pollCompletable(() -> this.toad.pollReq())
          .get();

        Middleware.Result result = Middleware.next();
        for (var f : this.middlewares) {
          if (!result.shouldContinue()) {
            break;
          }

          if (result.isAsync()) {
            try {
              result.response().get();
            } catch (Throwable e) {
              result = Middleware.error(e);
              break;
            }
            // Toad.ack(req);
          }

          try {
            result = f.apply(req);
          } catch (Throwable e) {
            result = Middleware.error(e);
          }
        }

        switch (result) {
          case Middleware.ResultExit e:
            this.toad.close();
            return;
          case Middleware.ResultError e:
            result = this.exceptionHandler.apply(req, e.error);
            break;
          case Middleware.ResultNextSync n:
            result = this.notFoundHandler.apply(req);
            break;
          case Middleware.ResultNextAsync n:
            result = this.notFoundHandler.apply(req);
            break;
          default:
            break;
        }

        req.close();

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
          Async
            .pollCompletable(() -> this.toad.sendMessage(resp.get().toOwned()))
            .get();
        }
      } catch (Throwable e) {
        Toad.logger().log(Level.SEVERE, e.toString());
      }
    }
  }

  public static final class Middleware {

    public static final CompletableFuture<Optional<Message>> noop =
      CompletableFuture.completedFuture(Optional.empty());

    public static final Function<Message, Result> notFound = m -> {
      return Middleware.respond(m.buildResponse().code(Code.NOT_FOUND).build());
    };

    public static final BiFunction<Message, Throwable, Result> debugExceptionHandler =
      (m, e) -> {
        Toad
          .logger()
          .log(
            Level.SEVERE,
            String.format("while handling %s", m.toDebugString()),
            e
          );

        var rep = m
          .buildResponse()
          .code(Code.INTERNAL_SERVER_ERROR)
          .payload(Payload.text(e.toString()))
          .build();

        return Middleware.respond(rep);
      };

    public static final BiFunction<Message, Throwable, Result> exceptionHandler =
      (m, e) -> {
        Toad
          .logger()
          .log(
            Level.SEVERE,
            String.format("while handling %s", m.toDebugString()),
            e
          );
        var rep = m.buildResponse().code(Code.INTERNAL_SERVER_ERROR).build();
        return Middleware.respond(rep);
      };

    public static Result respond(Message m) {
      return new ResultRespondSync(m);
    }

    public static Result respond(CompletableFuture<Message> m) {
      return new ResultRespondAsync(m);
    }

    public static Result error(Throwable e) {
      return new ResultError(e);
    }

    public static Result exit() {
      return new ResultExit();
    }

    public static Result next() {
      return new ResultNextSync();
    }

    public static Result next(CompletableFuture<Void> work) {
      return new ResultNextAsync(work);
    }

    public static sealed interface Result
      permits
        ResultExit,
        ResultError,
        ResultNextSync,
        ResultNextAsync,
        ResultRespondSync,
        ResultRespondAsync {
      public boolean shouldContinue();

      public boolean isAsync();

      public CompletableFuture<Optional<Message>> response();
    }

    public static final class ResultExit implements Result {

      public ResultExit() {}

      @Override
      public boolean shouldContinue() {
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

    public static final class ResultError implements Result {

      public final Throwable error;

      public ResultError(Throwable error) {
        this.error = error;
      }

      @Override
      public boolean shouldContinue() {
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

    public static final class ResultNextSync implements Result {

      public ResultNextSync() {}

      @Override
      public boolean shouldContinue() {
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

    public static final class ResultNextAsync implements Result {

      final CompletableFuture<Void> work;

      public ResultNextAsync(CompletableFuture<Void> work) {
        this.work = work;
      }

      @Override
      public boolean shouldContinue() {
        return true;
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
      public boolean shouldContinue() {
        return false;
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
      public boolean shouldContinue() {
        return false;
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
    final ArrayList<Function<Message, Middleware.Result>> middlewares =
      new ArrayList<>();
    Function<Message, Middleware.Result> notFoundHandler = Middleware.notFound;
    BiFunction<Message, Throwable, Middleware.Result> exceptionHandler =
      Middleware.exceptionHandler;

    Builder(Toad toad) {
      this.toad = toad;
    }

    public Builder middleware(Function<Message, Middleware.Result> f) {
      this.middlewares.add(f);
      return this;
    }

    public Builder when(
      Predicate<Message> pred,
      Function<Message, Middleware.Result> f
    ) {
      return this.middleware(m -> {
          if (pred.test(m)) {
            return f.apply(m);
          } else {
            return Middleware.next();
          }
        });
    }

    public Builder put(String path, Function<Message, Middleware.Result> f) {
      return this.when(
          m ->
            Code.eq.test(m.code(), Code.PUT) &&
            m
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty()),
          f
        );
    }

    public Builder post(String path, Function<Message, Middleware.Result> f) {
      return this.when(
          m ->
            Code.eq.test(m.code(), Code.POST) &&
            m
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty()),
          f
        );
    }

    public Builder delete(String path, Function<Message, Middleware.Result> f) {
      return this.when(
          m ->
            Code.eq.test(m.code(), Code.DELETE) &&
            m
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty()),
          f
        );
    }

    public Builder get(String path, Function<Message, Middleware.Result> f) {
      return this.when(
          m ->
            Code.eq.test(m.code(), Code.GET) &&
            m
              .getPath()
              .map(p -> p.matches(path))
              .orElse(path == null || path.isEmpty()),
          f
        );
    }

    public Builder tap(Consumer<Message> f) {
      return this.middleware(m -> {
          f.accept(m);
          return Middleware.next();
        });
    }

    public Builder tapAsync(Function<Message, CompletableFuture<?>> f) {
      return this.middleware(m -> {
          return Middleware.next(f.apply(m).thenAccept(_void -> {}));
        });
    }

    public Builder debugExceptions() {
      return this.exceptionHandler(Middleware.debugExceptionHandler);
    }

    public Builder exceptionHandler(
      BiFunction<Message, Throwable, Middleware.Result> handler
    ) {
      this.exceptionHandler = handler;
      return this;
    }

    public Builder notFoundHandler(
      Function<Message, Middleware.Result> handler
    ) {
      this.notFoundHandler = handler;
      return this;
    }

    public Server build() {
      return new Server(
        this.toad,
        this.middlewares,
        this.notFoundHandler,
        this.exceptionHandler
      );
    }
  }
}
