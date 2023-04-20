import scala.jdk.CollectionConverters.*
import dev.toad.msg.*
import dev.toad.msg.option.Host
import dev.toad.msg.option.Path
import dev.toad.msg.option.Query

class Message extends munit.FunSuite {
  test("Message.uri (coap://)") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://google.com/cheese/gruyere?foo=bar&bingus")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    val a = msg.addr.get
    val h = msg.getHost.get
    val p = msg.getPath.get
    val q = msg.getQuery.get.toMap.asScala

    assertEquals(a.getPort, 5683)
    assertEquals(h.toString, "google.com")
    assertEquals(p.toString, "cheese/gruyere")
    assertEquals(Seq.from(q.get("foo").get.asScala), Seq(Query.Value("bar")))
    assertEquals(Seq.from(q.get("bingus").get.asScala), Seq(Query.Value.empty))
  }

  test("Message.uri (coaps://)") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coaps://google.com/cheese/gruyere?foo=bar&bingus")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    val a = msg.addr.get
    assertEquals(a.getPort, 5684)
  }
}
