import dev.toad.Async
import java.util.Date
import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.time.Duration

class AsyncTest extends munit.FunSuite {
  test("loop() runs until LoopHandle.stop() invoked") {
    var counter = 0;
    val loop = Async.loop(() => {
      counter += 1
    });

    val start = Date().getTime();
    while (counter < 10 && Date().getTime() < start + 1000) {
      Thread.sleep(Duration.ofMillis(10))
    }

    loop.stop()
    assert(counter >= 10)
  }

  test("LoopHandle.stop() prevents further runs") {
    var stopped = false;
    val loop = Async.loop(() => {
      if stopped then throw new Error()
    });

    loop.stop()
    stopped = true
  }

  test("LoopHandle.join() rethrows exceptions") {
    case class LoopThrew() extends Throwable {}
    case class LoopDidNotThrow() extends Throwable {}

    try {
      val loop = Async.loop((() => {
        throw LoopThrew()
      }): java.lang.Runnable)

      loop.join();
      throw LoopDidNotThrow()
    } catch {
      case LoopThrew() => {}
      case e           => throw e
    }
  }

  test("pollCompletable polls then completes") {
    var polls = 0

    val fut = Async.pollCompletable(() => {
      polls = polls + 1
      if polls == 10 then {
        Optional.of(true)
      } else {
        Optional.empty
      }
    })

    fut.get(200, TimeUnit.MILLISECONDS)
  }

  test("pollCompletable does not poll after completion") {
    var polls = 0
    val fut = Async.pollCompletable(() => {
      polls = polls + 1
      Optional.of(true)
    })

    fut.get()

    Thread.sleep(100)
    assertEquals(polls, 1)
  }

  test("pollCompletable completesExceptionally when poll throws") {
    val err = Async
      .pollCompletable[Object](() => throw Exception("foo"))
      .handle((ok, e) => {
        if ok != null then {
          "CompletableFuture was completed without exception"
        } else {
          e.getMessage
        }
      })
      .get()

    assertEquals(err, "java.lang.Exception: foo")
  }
}
