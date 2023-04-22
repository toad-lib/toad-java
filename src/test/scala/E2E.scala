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
import java.util.Optional
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.net.InetAddress

val logLevel = Level.INFO;
val serverPort = 10102;
val clientPort = 10101;

class E2E extends munit.FunSuite {
  val client = new Fixture[Client]("client") {
    private var client: Client = null;

    def apply() = this.client

    override def beforeAll(): Unit = {
      this.client =
        Toad.builder.port(clientPort.shortValue).logLevel(logLevel).buildClient
    }

    override def afterAll(): Unit = {
      this.client.close()
      this.client = null
    }
  }

  val server = new Fixture[Server]("server") {
    private var thread: Thread = null;
    private var server: Server = null;

    def apply() = this.server

    override def beforeAll(): Unit = {
      Toad.loadNativeLib()

      var counterN = 0
      this.server = Toad.builder
        .port(serverPort.shortValue)
        .logLevel(logLevel)
        .server
        .put(
          "failing",
          _msg => {
            throw java.lang.RuntimeException("fart")
          }
        )
        .get(
          "counter",
          msg => {
            counterN += 1
            Optional.of(
              msg.buildResponse
                .code(Code.OK_CONTENT)
                .payload(Payload.text(s"$counterN"))
                .build
            )
          }
        )
        .get(
          "greetings",
          msg => {
            Optional.of(
              msg.buildResponse
                .code(Code.OK_CONTENT)
                .payload(Payload.text(s"Hello, ${msg.payload.toString}!"))
                .build
            )
          }
        )
        .build

      this.thread = this.server.run()
    }

    override def afterAll(): Unit = {
      this.server.exit()
      this.thread.join()
      this.server.close()
    }
  }

  override def munitFixtures = List(client, server)

  test("server responds 2.05 Ok Content") {
    server()

    val rep = client()
      .get(
        Type.NON,
        s"coap://localhost:$serverPort/greetings",
        Payload.text("Fred")
      )
      .get()

    assertNoDiff(
      rep.payload.toString,
      "Hello, Fred!"
    )
    assertEquals(
      rep.code,
      Code.OK_CONTENT
    )
  }

  test(
    "ClientObserveStream yields new resource states when server is notified of new state"
  ) {
    val stream =
      client()
        .observe(
          Message.builder
            .uri(s"coap://localhost:$serverPort/counter")
            .`type`(Type.NON)
            .code(Code.GET)
            .build
        );

    try {
      assertNoDiff(stream.next().get().payload.toString, "1")
      server().notify("counter")
      assertNoDiff(stream.next().get().payload.toString, "2")
      server().notify("counter")
      assertNoDiff(stream.next().get().payload.toString, "3")
    } finally {
      stream.close()
    }
  }

  test("unregistered resource responds 4.04 Not Found") {
    server()
    val c = client()
    assertEquals(
      c.get(Type.NON, s"coap://localhost:$serverPort/not-found").get().code,
      Code.NOT_FOUND
    )
  }

  test("handler throwing exception responds 5.00 Internal Server Error") {
    server()
    val c = client()
    assertEquals(
      c.put(Type.NON, s"coap://localhost:$serverPort/failing").get().code,
      Code.INTERNAL_SERVER_ERROR
    )
  }
}
