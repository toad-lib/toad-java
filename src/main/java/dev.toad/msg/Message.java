package dev.toad.msg;

import dev.toad.msg.option.Path;
import dev.toad.msg.option.Host;
import dev.toad.msg.option.Query;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

public interface Message {
  public Optional<InetSocketAddress> addr();

  public Id id();

  public Token token();

  public Code code();

  public Type type();

  public List<Option> options();

  default public Optional<Option> getOption(long number) {
    return this.options().stream().filter(o -> o.number() == number).findAny();
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

  public byte[] payloadBytes();

  public String payloadString();

  public dev.toad.msg.owned.Message toOwned();

  public byte[] toBytes();
}
