package dev.toad.msg;

import dev.toad.ffi.Ptr;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A pointer to a [`toad_msg::Message`](https://docs.rs/toad-msg/latest/toad_msg/struct.Message.html)
 *
 * Note that the pointer is not guaranteed to continue to be valid once
 * control is yielded back to the rust runtime, meaning instances of
 * MessageRef should never be stored in state; invoke `.clone()` first.
 */
public class MessageRef implements Message, AutoCloseable {

  private Ptr ptr;

  private Optional<InetSocketAddress> source = Optional.empty();

  private static native InetSocketAddress source(long addr);

  private static native int id(long addr);

  private static native byte[] token(long addr);

  private static native byte[] payload(long addr);

  private static native MessageCode code(long addr);

  private static native MessageType type(long addr);

  private static native MessageOptionRef[] opts(long addr);

  public MessageRef(long addr) {
    this.ptr = Ptr.register(this.getClass(), addr);
  }

  public Message clone() {
    return new MessageOwned(this);
  }

  public InetSocketAddress source() {
    if (this.source.isEmpty()) {
      this.source = Optional.of(this.source(this.ptr.addr()));
    }

    return this.source.get();
  }

  public int id() {
    return this.id(this.ptr.addr());
  }

  public byte[] token() {
    return this.token(this.ptr.addr());
  }

  public MessageCode code() {
    return this.code(this.ptr.addr());
  }

  public MessageType type() {
    return this.type(this.ptr.addr());
  }

  public MessageOptionRef[] optionRefs() {
    return this.opts(this.ptr.addr());
  }

  public List<MessageOption> options() {
    return Arrays.asList(this.opts(this.ptr.addr()));
  }

  public byte[] payloadBytes() {
    return this.payload(this.ptr.addr());
  }

  public String payloadString() {
    return new String(this.payload(this.ptr.addr()));
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
