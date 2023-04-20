import dev.toad.*
import dev.toad.msg.*
import dev.toad.msg.option.ContentFormat
import dev.toad.msg.option.Accept
import mock.java.nio.channels.Mock
import java.net.InetSocketAddress
import java.util.logging.Logger
import java.util.logging.Level
import java.util.ArrayList
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class E2E extends munit.FunSuite {
  test("minimal client and server") {
    Toad.loadNativeLib()

    val mock = Mock.Channel()

    val resp = dev.toad.msg.build.Message
      .builder()
      .addr(InetSocketAddress("127.0.0.1", 1111))
      .`type`(Type.ACK)
      .code(Code.OK_CONTENT)
      .id(Id(2))
      .token(Token(Array(1)))
      .payload(Payload.text("foobar"))
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

    val client = Toad.builder.channel(mock).logLevel(Level.INFO).buildClient
    val respFuture = client.send(req)

    var bufs = ArrayList[ByteBuffer]()
    bufs.add(ByteBuffer.wrap(resp.toBytes()))
    mock.recv.put(InetSocketAddress("127.0.0.1", 2222), bufs)

    val respActual = respFuture.get(1, TimeUnit.SECONDS)

    assertEquals(resp.payload.bytes.toSeq, respActual.payload.bytes.toSeq)
  }
}
