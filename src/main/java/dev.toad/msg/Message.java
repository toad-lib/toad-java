package dev.toad.msg;

import java.net.InetSocketAddress;
import java.util.List;

public interface Message {
  public InetSocketAddress source();

  public Id id();

  public Token token();

  public Code code();

  public Type type();

  public List<Option> options();

  public byte[] payloadBytes();

  public String payloadString();
}
