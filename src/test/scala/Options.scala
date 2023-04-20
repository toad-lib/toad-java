import scala.collection.immutable.HashMap
import scala.jdk.CollectionConverters.*
import dev.toad.msg.option.Host
import dev.toad.msg.option.Path
import dev.toad.msg.option.Query

class Options extends munit.FunSuite {
  test("Path.toString") {
    val p = Path("foo/bar/baz")
    assertEquals(p.toString, "foo/bar/baz")
  }

  test("Path.segments") {
    val p = Path("foo/bar/baz")
    assertEquals(Seq.from(p.segments.asScala), Seq("foo", "bar", "baz"))
  }

  test("Host.toString") {
    val h = Host("cheese.com")
    assertEquals(h.toString, "cheese.com")
  }

  test("Query.equals") {
    val q = Query(
      Seq("foo=test", "bar", "quux=a", "bar", "quux=b", "bar=third").asJava
    )

    assertEquals(q.toString, "foo=test&bar&quux=a&bar&quux=b&bar=third")
  }

  test("Query.toMap") {
    val q = Query(
      Seq("foo=test", "bar", "quux=a", "bar", "quux=b", "bar=third").asJava
    )

    val actual = q.toMap.asScala

    assertEquals(
      Map.from(
        actual.map { case (k, vs) => (k, Seq.from(vs.asScala)) }
      ),
      Map(
        "foo" -> Seq(Query.Value("test")),
        "bar" -> Seq(Query.Value.empty, Query.Value.empty, Query.Value("third")),
        "quux" -> Seq(Query.Value("a"), Query.Value("b")),
      )
    )
  }
}
