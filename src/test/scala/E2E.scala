import dev.toad.*
import dev.toad.msg.*
import dev.toad.msg.option.ContentFormat
import dev.toad.msg.option.Accept
import mock.java.nio.channels.Mock
import java.lang.Thread
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.logging.Logger
import java.util.logging.Level
import java.util.ArrayList
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.net.InetAddress

class E2E extends munit.FunSuite {
  test("minimal client and server") {
    Toad.loadNativeLib()

    val serverThread = Thread((() => {
      Toad.builder
        .port(10102)
        .logLevel(Level.INFO)
        .server
        .post(
          "exit",
          _msg => {
            Server.Middleware.exit
          }
        )
        .get(
          "hello",
          msg => {
            val name = msg.payload.toString
            val rep = msg.modify.unsetId
              .`type`(Type.NON)
              .code(Code.OK_CONTENT)
              .payload(Payload.text(s"Hello, $name!"))
              .build
            Server.Middleware.respond(rep)
          }
        )
        .build
        .run
    }): java.lang.Runnable)

    serverThread.start()

    val req = Message.builder
      .uri("coap://localhost:10102/hello")
      .`type`(Type.NON)
      .code(Code.GET)
      .payload(Payload.text("Fred"))
      .build

    val client = Toad.builder.port(10101).logLevel(Level.INFO).buildClient
    val respFuture = client.send(req)

    try {
      val respActual = respFuture.get(1, TimeUnit.SECONDS)
      assertNoDiff(respActual.payload.toString, "Hello, Fred!")
    } finally {
      val exit = Message.builder
        .uri("coap://localhost:10102/exit")
        .`type`(Type.NON)
        .code(Code.POST)
        .build
      client.sendNoResponse(exit)
    }

    serverThread.join
  }
}
