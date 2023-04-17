import dev.toad.*
import dev.toad.msg.*
import dev.toad.msg.option.ContentFormat
import dev.toad.msg.option.Accept
import mock.java.nio.channels.Mock
import java.net.InetSocketAddress
import java.util.ArrayList
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class E2E extends munit.FunSuite {
  test("minimal client and server") {
    Toad.loadNativeLib()

    val mock = Mock.Channel()

    val ack = dev.toad.msg.build.Message
      .builder()
      .addr(InetSocketAddress("127.0.0.1", 1111))
      .`type`(Type.ACK)
      .code(Code.EMPTY)
      .id(Id(2))
      .token(Token(Array(1)))
      .option(ContentFormat.TEXT)
      .payload("foobar")
      .build

    val resp = dev.toad.msg.build.Message
      .builder()
      .addr(InetSocketAddress("127.0.0.1", 1111))
      .`type`(Type.NON)
      .code(Code.OK_CONTENT)
      .id(Id(3))
      .token(Token(Array(1)))
      .option(ContentFormat.TEXT)
      .payload("foobar")
      .build

    val req = dev.toad.msg.build.Message
      .builder()
      .addr(InetSocketAddress("127.0.0.1", 2222))
      .`type`(Type.CON)
      .code(Code(2, 4))
      .id(Id(1))
      .token(Token(Array(1)))
      .option(Accept.TEXT)
      .build

    val client = Toad.builder.channel(mock).buildClient
    val respFuture = client.send(req)

    var bufs = ArrayList[ByteBuffer]()
    bufs.add(ByteBuffer.wrap(resp.toBytes()))
    mock.recv.put(InetSocketAddress("127.0.0.1", 2222), bufs)

    val respActual = respFuture.get(1, TimeUnit.SECONDS)

    assertEquals(resp.payloadBytes().toSeq, respActual.payloadBytes().toSeq)
  }
}
