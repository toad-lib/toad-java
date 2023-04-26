import dev.toad.*
import dev.toad.msg.*
import dev.toad.msg.option.ContentFormat
import dev.toad.msg.option.Accept
import mock.java.nio.channels.Mock
import java.lang.Thread
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.logging.Logger
import java.util.logging.LogRecord
import java.util.logging.Level
import java.util.logging.Formatter
import java.util.ArrayList
import java.util.Optional
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.net.InetAddress

val logLevel = Level.OFF;
val serverPort = 10102;
val clientPort = 10101;
val client2Port = 10100;

val totalWidth = 120
val padding = 1
val sep = "||"
val startMillis = java.time.Instant.now.toEpochMilli

enum Position:
  case Left, Center, Right

def lines(pos: Position, r: LogRecord): String = {
  val width = (totalWidth / 3).intValue
  val logger = r.getLoggerName
  val level = r.getLevel.toString
  val message =
    s"[$logger($level)] ${r.getMillis - startMillis}\n${r.getMessage}"

  0.until(message.length)
    .foldLeft(Seq(""))((lines, ix) => {
      val lines_ = lines.init
      val last = lines.last
      (last, String.valueOf(message.charAt(ix))) match {
        case (_, "\n")                         => lines :+ ""
        case (line, s) if line.length == width => lines :+ s
        case (line, s)                         => lines_ :+ (line ++ s)
      }
    })
    .map(lineText => line(pos, lineText))
    .foldLeft("")((m, line) => s"$m$line\n") ++ "\n"
}

def line(pos: Position, text: String): String = {
  val width = (totalWidth / 2).intValue
  val empty = " ".repeat(width)
  val after = " ".repeat(width - text.length)
  val pad = " ".repeat(padding)
  pos match {
    case Position.Left   => s"$text$after$pad$sep$pad$empty$pad$sep"
    case Position.Center => s"$empty$pad$sep$pad$text$after$pad$sep"
    case Position.Right  => s"$empty$pad$sep$pad$empty$pad$sep$pad$text"
  }
}

class ServerLogFormatter extends Formatter {
  override def format(r: LogRecord): String = {
    lines(Position.Left, r)
  }
}

class Client1LogFormatter extends Formatter {
  override def format(r: LogRecord): String = {
    lines(Position.Center, r)
  }
}

class Client2LogFormatter extends Formatter {
  override def format(r: LogRecord): String = {
    lines(Position.Right, r)
  }
}

class E2E extends munit.FunSuite {
  val client = new Fixture[Client]("client") {
    private var client: Client = null;

    def apply() = this.client

    override def beforeAll(): Unit = {
      this.client = Toad.builder
        .port(clientPort.shortValue)
        .logLevel(logLevel)
        .loggerName("client")
        .logFormatter(Client1LogFormatter())
        .buildClient
    }

    override def afterAll(): Unit = {
      this.client.close()
      this.client = null
    }
  }

  val client2 = new Fixture[Client]("client2") {
    private var client: Client = null;

    def apply() = this.client

    override def beforeAll(): Unit = {
      this.client = Toad.builder
        .port(client2Port.shortValue)
        .logLevel(logLevel)
        .loggerName("client2")
        .logFormatter(Client2LogFormatter())
        .buildClient
    }

    override def afterAll(): Unit = {
      this.client.close()
      this.client = null
    }
  }

  val server = new Fixture[Server]("server") {
    private var server: Server = null;

    def apply() = this.server

    override def beforeAll(): Unit = {
      Toad.loadNativeLib()

      var number = AtomicInteger(0)
      this.server = Toad.builder
        .port(serverPort.shortValue)
        .logLevel(logLevel)
        .loggerName("server")
        .logFormatter(ServerLogFormatter())
        .server
        .put(
          "failing",
          _msg => {
            throw java.lang.RuntimeException("fart")
          }
        )
        .post(
          "number",
          msg => {
            number.set(Integer.decode(msg.payload.toString))
            this.server.notify("number")
            Optional.of(msg.buildResponse.code(Code.OK_CHANGED).build)
          }
        )
        .get(
          "number",
          msg => {
            Optional.of(
              msg.buildResponse
                .code(Code.OK_CONTENT)
                .payload(Payload.text(s"${number.get()}"))
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

      this.server.run()
    }

    override def afterAll(): Unit = {
      this.server.close()
    }
  }

  override def munitFixtures = List(client, client2, server)

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
    val n = AtomicInteger(-1)
    val subscription = client()
      .observe(
        Message.builder
          .uri(s"coap://localhost:$serverPort/number")
          .`type`(Type.NON)
          .code(Code.GET)
          .build
      )
      .subscribe(rep => {
        n.set(Integer.decode(rep.payload.toString))
      })

    def shouldBe(x: Int) =
      Async
        .pollCompletable(() => {
          Optional.of(Object()).filter({ case _ => n.get() == x })
        })
        .get(1, TimeUnit.SECONDS)

    shouldBe(0)
    client2()
      .post(
        Type.CON,
        s"coap://localhost:$serverPort/number",
        Payload.text(1.toString)
      )
      .get()
    shouldBe(1)
    client2()
      .post(
        Type.CON,
        s"coap://localhost:$serverPort/number",
        Payload.text(2.toString)
      )
      .get()
    shouldBe(2)
    client2()
      .post(
        Type.CON,
        s"coap://localhost:$serverPort/number",
        Payload.text(3.toString)
      )
      .get()
    shouldBe(3)
    client2()
      .post(
        Type.CON,
        s"coap://localhost:$serverPort/number",
        Payload.text(4.toString)
      )
      .get()

    subscription.stop()
    subscription.close()
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
