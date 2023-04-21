import dev.toad.Toad
import dev.toad.msg.*
import dev.toad.msg.option.*

class Debug extends munit.FunSuite {
  test("Message") {
    Toad.loadNativeLib()

    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost/foo/bar/baz?quux&sling=shot")
      .`type`(Type.NON)
      .code(Code.GET)
      .payload(Payload.json("[\"fart\"]"))
      .option(Accept.TEXT)
      .build

    assertNoDiff(
      msg.toDebugString,
      Seq(
        "NON GET coap://127.0.0.1:5683/foo/bar/baz?quux&sling=shot",
        "  Id(0) Token([])",
        "  Accept: text/plain; charset=utf-8",
        "  Uri-Host: 127.0.0.1",
        "  Uri-Path: foo/bar/baz",
        "  Content-Format: application/json",
        "  Uri-Query: quux&sling=shot",
        "",
        "[\"fart\"]"
      )
        .foldLeft("")((b, a) => b ++ "\n" ++ a)
    )
  }
}
