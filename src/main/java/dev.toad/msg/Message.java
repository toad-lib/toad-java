package dev.toad.msg;

import dev.toad.msg.option.Accept;
import dev.toad.msg.option.ContentFormat;
import dev.toad.msg.option.Path;
import dev.toad.msg.option.Host;
import dev.toad.msg.option.Query;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

public interface Message {
  public static dev.toad.msg.build.MessageNeeds.Destination builder() {
    return dev.toad.msg.build.Message.builder();
  }

  public Optional<InetSocketAddress> addr();

  public Id id();

  public Token token();

  public Code code();

  public Type type();

  public List<Option> options();

  public byte[] payloadBytes();

  public String payloadString();

  public dev.toad.msg.owned.Message toOwned();

  public byte[] toBytes();

  default public Optional<Option> getOption(long number) {
    return this.options().stream().filter(o -> o.number() == number).findAny();
  }

  default public Optional<Accept> getAccept() {
    return this.getOption(Accept.number).map(o -> new Accept(o));
  }

  default public Optional<ContentFormat> getContentFormat() {
    return this.getOption(ContentFormat.number).map(o -> new ContentFormat(o));
  }

  default public Optional<Path> getPath() {
    return this.getOption(Path.number).map(o -> new Path(o));
  }

  default public Optional<Host> getHost() {
    return this.getOption(Host.number).map(o -> new Host(o));
  }

  default public Optional<Query> getQuery() {
    return this.getOption(Query.number).map(o -> new Query(o));
  }
}
