import scala.jdk.CollectionConverters.*
import dev.toad.*
import dev.toad.msg.*
import dev.toad.msg.option.*

class Eq extends munit.FunSuite {
  test("byteArray") {
    assert(dev.toad.Eq.byteArray.test(Array[Byte](), Array[Byte]()))
    assert(dev.toad.Eq.byteArray.test(Array[Byte](1), Array[Byte](1)))
    assert(dev.toad.Eq.byteArray.test(Array[Byte](1, 2), Array[Byte](1, 2)))
    assert(!dev.toad.Eq.byteArray.test(Array[Byte](1), Array[Byte]()))
  }

  test("int, short, long") {
    assert(dev.toad.Eq.int_.test(0, 0))
    assert(dev.toad.Eq.int_.test(1, 1))
    assert(dev.toad.Eq.long_.test(1, 1))
    assert(dev.toad.Eq.short_.test(1.shortValue, 1.shortValue))
    assert(!dev.toad.Eq.int_.test(1, 0))
    assert(!dev.toad.Eq.long_.test(1, 0))
    assert(!dev.toad.Eq.short_.test(1.shortValue, 0.shortValue))
  }

  test("String") {
    assert(dev.toad.Eq.string.test(null, null))
    assert(dev.toad.Eq.string.test("a", "a"))
    assert(!dev.toad.Eq.string.test("", null))
    assert(!dev.toad.Eq.string.test("", "a"))
  }

  test("contramap") {
    class Foo(val bar: String)

    val a = Foo("baz")
    val b = Foo("bingo")

    val eq = dev.toad.Eq.string.contramap((foo: Foo) => foo.bar)

    assert(eq.test(a, a))
    assert(!eq.test(a, b))
  }

  test("contramap + all") {
    class Foo(val bar: String, val baz: String)

    val a = Foo("a", "b")
    val b = Foo("b", "c")
    val c = Foo("a", "c")

    val eq = dev.toad.Eq.all(
      Seq(
        dev.toad.Eq.string.contramap((f: Foo) => f.bar),
        dev.toad.Eq.string.contramap((f: Foo) => f.baz)
      ).asJava
    )

    assert(eq.test(a, a))
    assert(eq.test(b, b))
    assert(!eq.test(a, b))
    assert(!eq.test(a, c))
  }

  test("Map") {
    val map = dev.toad.Eq.map[String, String](dev.toad.Eq.string)
    assert(map.test(Map().asJava, Map().asJava))
    assert(
      map.test(
        Map("a" -> "a", "foo" -> "bar").asJava,
        Map("a" -> "a", "foo" -> "bar").asJava
      )
    )
    assert(
      !map.test(
        Map("foo" -> "bar").asJava,
        Map("a" -> "a", "foo" -> "bar").asJava
      )
    )
    assert(!map.test(Map().asJava, Map("a" -> "a", "foo" -> "bar").asJava))
  }

  test("List") {
    val list = dev.toad.Eq.list(dev.toad.Eq.string)
    assert(list.test(Seq("a", "b").asJava, Seq("a", "b").asJava))
    assert(!list.test(Seq("b", "b").asJava, Seq("a", "b").asJava))
  }

  test("Id") {
    assert(Id.eq.test(Id(1), Id(1)))
    assert(!Id.eq.test(Id(0), Id(1)))
  }

  test("Token") {
    assert(
      Token.eq.test(Token(Array[Byte](1, 2, 3)), Token(Array[Byte](1, 2, 3)))
    )
    assert(
      !Token.eq.test(Token(Array[Byte](2, 2, 3)), Token(Array[Byte](1, 2, 3)))
    )
  }

  test("Code") {
    assert(Code.eq.test(Code.OK_CONTENT, Code.OK_CONTENT))
    assert(!Code.eq.test(Code.GET, Code.OK_CONTENT))
  }

  test("Message") {
    val a = Message.builder
      .uri("coap://localhost:1234/a/b/c")
      .`type`(Type.CON)
      .code(Code.GET)
      .id(Id(0))
      .token(Token(Array[Byte](1, 2, 3)))
      .payload(Payload.text("foo"))
      .build
    assert(Message.eq.test(a, a))
    assert(!Message.eq.test(a, a.buildCopy.`type`(Type.NON).build))
    assert(!Message.eq.test(a, a.buildCopy.option(Path("b/c")).build))
    assert(!Message.eq.test(a, a.buildCopy.code(Code.PUT).build))
    assert(!Message.eq.test(a, a.buildCopy.id(Id(1)).build))
    assert(!Message.eq.test(a, a.buildCopy.token(Token(Array[Byte](2))).build))
    assert(
      !Message.eq.test(a, a.buildCopy.uri("coap://google.com:1234/a/b/c").build)
    )
    assert(!Message.eq.test(a, a.buildCopy.payload(Payload.json("foo")).build))
  }
}
