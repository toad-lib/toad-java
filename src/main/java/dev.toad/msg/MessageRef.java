package dev.toad.msg;

import java.util.Arrays;
import java.util.List;

/**
 * A pointer to a [`toad_msg::Message`](https://docs.rs/toad-msg/latest/toad_msg/struct.Message.html)
 *
 * Note that the pointer is not guaranteed to continue to be valid once
 * control is yielded back to the rust runtime, meaning instances of
 * MessageRef should never be stored in state; invoke `.clone()` first.
 */
public class MessageRef implements Message {

  private final long addr;

  private static native int id(long addr);

  private static native byte[] token(long addr);

  private static native byte[] payload(long addr);

  private static native MessageCode code(long addr);

  private static native MessageType type(long addr);

  private static native MessageOptionRef[] opts(long addr);

  public MessageRef(long addr) {
    this.addr = addr;
  }

  public Message clone() {
    return new MessageOwned(this);
  }

  public int id() {
    return this.id(this.addr);
  }

  public byte[] token() {
    return this.token(this.addr);
  }

  public MessageCode code() {
    return this.code(this.addr);
  }

  public MessageType type() {
    return this.type(this.addr);
  }

  public MessageOptionRef[] optionRefs() {
    return this.opts(this.addr);
  }

  public List<MessageOption> options() {
    return Arrays.asList(this.opts(this.addr));
  }

  public byte[] payloadBytes() {
    return this.payload(this.addr);
  }

  public String payloadString() {
    return new String(this.payload(this.addr));
  }
}
