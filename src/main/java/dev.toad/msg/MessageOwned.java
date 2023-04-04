package dev.toad.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageOwned implements Message {

  private static int id;
  private static byte[] token;
  private static byte[] payload;
  private static MessageCode code;
  private static MessageType type;
  private static List<MessageOption> opts;

  public MessageOwned(MessageRef ref) {
    this.id = ref.id();
    this.token = ref.token();
    this.code = ref.code();
    this.type = ref.type();
    this.payload = ref.payloadBytes().clone();

    this.opts =
      Arrays
        .asList(ref.optionRefs())
        .stream()
        .map(MessageOptionRef::clone)
        .collect(Collectors.toList());
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
