package dev.toad.msg;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageOwned implements Message {

  private final InetSocketAddress source;
  private final int id;
  private final byte[] token;
  private final byte[] payload;
  private final MessageCode code;
  private final MessageType type;
  private final List<MessageOption> opts;

  public MessageOwned(MessageRef ref) {
    this.id = ref.id();
    this.token = ref.token();
    this.code = ref.code();
    this.type = ref.type();
    this.payload = ref.payloadBytes().clone();
    this.source = ref.source();

    this.opts =
      Arrays
        .asList(ref.optionRefs())
        .stream()
        .map(MessageOptionRef::clone)
        .collect(Collectors.toList());
  }

  public InetSocketAddress source() {
    return this.source;
  }

  public int id() {
    return this.id;
  }

  public byte[] token() {
    return this.token;
  }

  public MessageCode code() {
    return this.code;
  }

  public MessageType type() {
    return this.type;
  }

  public List<MessageOption> options() {
    return this.opts;
  }

  public byte[] payloadBytes() {
    return this.payload;
  }

  public String payloadString() {
    return new String(this.payload);
  }
}
