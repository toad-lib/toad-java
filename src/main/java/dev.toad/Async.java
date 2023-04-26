package dev.toad;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class Async {

  public static ScheduledExecutorService executor =
    Executors.newScheduledThreadPool(32);

  public static class LoopHandle implements AutoCloseable {

    CompletableFuture<Object> fut = null;
    final AtomicBoolean break_ = new AtomicBoolean(false);
    final Runnable onStop;

    LoopHandle(Runnable onStop) {
      this.onStop = onStop;
    }

    public void join() throws Throwable, InterruptedException {
      try {
        this.fut.get();
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }

    public void stop() throws Throwable, InterruptedException {
      this.break_.set(true);
      this.join();
      this.onStop.run();
    }

    @Override
    public void close() {
      try {
        this.stop();
      } catch (Throwable t) {}
    }
  }

  public static LoopHandle loop(Runnable f) {
    return Async.loop(f, () -> {});
  }

  public static LoopHandle loop(Runnable f, Runnable onStop) {
    var handle = new LoopHandle(onStop);
    var fut = Async.pollCompletable(() -> {
      if (handle.break_.get()) {
        return Optional.of(new Object());
      } else {
        f.run();
        return Optional.empty();
      }
    });

    handle.fut = fut;

    return handle;
  }

  public static <T> CompletableFuture<T> pollCompletable(
    Supplier<Optional<T>> sup
  ) {
    var fut = new CompletableFuture();
    var pollTask = Async.executor.scheduleAtFixedRate(
      () -> {
        try {
          var t = sup.get();
          if (!t.isEmpty()) {
            fut.complete(t.get());
          }
        } catch (Throwable ex) {
          fut.completeExceptionally(ex);
        }
      },
      0,
      10,
      TimeUnit.MILLISECONDS
    );

    return fut.whenComplete((t, err) -> {
      pollTask.cancel(true);
    });
  }
}
