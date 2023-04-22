package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import dev.toad.msg.option.Accept;
import dev.toad.msg.option.ContentFormat;
import dev.toad.msg.option.Host;
import dev.toad.msg.option.Path;
import dev.toad.msg.option.Query;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public interface Message extends Debug {
  public static dev.toad.msg.build.MessageNeeds.Destination builder() {
    return dev.toad.msg.build.Message.builder();
  }

  public Optional<InetSocketAddress> addr();

  public Id id();

  public Token token();

  public Code code();

  public Type type();

  public List<Option> options();

  public Payload payload();

  public dev.toad.msg.owned.Message toOwned();

  public byte[] toBytes();

  public default dev.toad.msg.build.Message buildCopy() {
    return dev.toad.msg.build.Message.copyOf(this);
  }

  public default dev.toad.msg.build.MessageNeeds.Code buildResponse() {
    return dev.toad.msg.build.Message.respondTo(this);
  }

  public default Optional<Option> getOption(long number) {
    return this.options().stream().filter(o -> o.number() == number).findAny();
  }

  public default Optional<Accept> getAccept() {
    return this.getOption(Accept.number).map(o -> new Accept(o));
  }

  public default Optional<ContentFormat> getContentFormat() {
    return this.getOption(ContentFormat.number).map(o -> new ContentFormat(o));
  }

  public default Optional<Path> getPath() {
    return this.getOption(Path.number).map(o -> new Path(o));
  }

  public default Optional<Host> getHost() {
    return this.getOption(Host.number).map(o -> new Host(o));
  }

  public default Optional<Query> getQuery() {
    return this.getOption(Query.number).map(o -> new Query(o));
  }

  public default URI uri() {
    int port = this.addr().map(a -> a.getPort()).orElse(5683);
    String scheme = port == 5684 ? "coaps" : "coap";
    String hostAddr =
      this.addr().map(a -> a.getAddress().getHostAddress()).orElse(null);
    String host =
      this.getHost()
        .map(h -> h.toString())
        .filter(h -> h != null && !h.trim().isEmpty())
        .orElse(hostAddr);
    String path =
      this.getPath()
        .map(p -> p.toString())
        .map(p -> p.startsWith("/") ? p : "/" + p)
        .orElse(null);
    String query = this.getQuery().map(q -> q.toString()).orElse(null);

    try {
      return new URI(
        scheme,
        /* userInfo */null,
        host,
        port,
        path,
        query,
        /* fragment */null
      );
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Eq<Message> eq() {
    return Eq.all(
      List.of(
        Eq.optional(Eq.socketAddress).contramap(m -> m.addr()),
        Eq.list(Option.eq()).contramap(m -> m.options()),
        Code.eq.contramap(m -> m.code()),
        Id.eq.contramap(m -> m.id()),
        Token.eq.contramap(m -> m.token()),
        Type.eq.contramap(m -> m.type()),
        Payload.eq.contramap(m -> m.payload())
      )
    );
  }

  public default boolean equals(Message m) {
    return Message.eq().test(this, m);
  }

  @Override
  public default String toDebugString() {
    return (
      this.type().toDebugString() +
      " " +
      this.code().toDebugString() +
      " " +
      this.uri().toString() +
      "\n  " +
      this.id().toDebugString() +
      " " +
      this.token().toDebugString() +
      this.options()
        .stream()
        .map(Debug::toDebugString)
        .reduce("", (b, a) -> b + "\n  " + a) +
      "\n" +
      (!this.payload().isEmpty() ? "\n" + this.payload().toDebugString() : "")
    );
  }
}
