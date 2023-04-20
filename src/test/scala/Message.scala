import scala.jdk.CollectionConverters.*
import dev.toad.msg.*
import dev.toad.msg.option.ContentFormat
import dev.toad.msg.option.Host
import dev.toad.msg.option.Path
import dev.toad.msg.option.Query

class MessageBuilder extends munit.FunSuite {
  test("payload sets content format") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost")
      .`type`(Type.NON)
      .code(Code.GET)
      .payload(Payload.json("[\"fart\"]"))
      .build

    assertEquals(msg.getContentFormat.get, ContentFormat.JSON)
  }

  test("uri uses system DNS to resolve host address") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    assertEquals(msg.addr.get.getAddress.getHostAddress, "127.0.0.1");
  }

  test("uri gets port from URI") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost:1234")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    assertEquals(msg.addr.get.getPort, 1234)
  }

  test("uri gets port 5683 from scheme coap://") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    assertEquals(msg.addr.get.getPort, 5683)
  }

  test("uri gets port 5684 from scheme coaps://") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coaps://localhost/cheese/gruyere?foo=bar&bingus")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    assertEquals(msg.addr.get.getPort, 5684)
  }

  test("uri sets host to host section of uri") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost/cheese/gruyere?foo=bar&bingus")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    assertEquals(msg.getHost.get.toString, "localhost")
  }

  test("uri sets path to path section of uri") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost/cheese/gruyere?foo=bar&bingus")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    assertEquals(
      Seq.from(msg.getPath.get.segments.asScala),
      Seq("cheese", "gruyere")
    )
  }

  test("uri sets query to query section of uri") {
    val msg = dev.toad.msg.build.Message
      .builder()
      .uri("coap://localhost/cheese/gruyere?foo=bar&bingus")
      .`type`(Type.NON)
      .code(Code.GET)
      .build

    val q = msg.getQuery.get.toMap.asScala
    assertEquals(Seq.from(q.get("foo").get.asScala), Seq(Query.Value("bar")))
    assertEquals(Seq.from(q.get("bingus").get.asScala), Seq(Query.Value.empty))
  }
}
