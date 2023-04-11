package dev.toad.msg;

import java.net.InetSocketAddress;
import java.util.List;

public interface Message {
  public InetSocketAddress source();

  public int id();

  public byte[] token();

  public MessageCode code();

  public MessageType type();

  public List<MessageOption> options();

  public byte[] payloadBytes();

  public String payloadString();
}
