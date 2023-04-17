import dev.toad.Async
import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture

class AsyncTest extends munit.FunSuite {
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
