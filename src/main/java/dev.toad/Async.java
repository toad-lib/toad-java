package dev.toad;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Async {

  public static ScheduledExecutorService executor =
    Executors.newSingleThreadScheduledExecutor();

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
