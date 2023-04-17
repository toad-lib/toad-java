package dev.toad.msg;

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

  public byte[] payloadBytes();

  public String payloadString();

  public dev.toad.msg.owned.Message toOwned();

  public byte[] toBytes();
}
