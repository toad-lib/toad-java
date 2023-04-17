package dev.toad.msg.ref;

import dev.toad.ffi.Ptr;
import dev.toad.msg.*;
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
public final class Message implements dev.toad.msg.Message, AutoCloseable {

  Ptr ptr;

  Optional<InetSocketAddress> addr = Optional.empty();

  static native InetSocketAddress addr(long addr);

  static native Id id(long addr);

  static native Token token(long addr);

  static native byte[] payload(long addr);

  static native Code code(long addr);

  static native Type typ(long addr);

  static native dev.toad.msg.ref.Option[] opts(long addr);

  static native byte[] toBytes(long addr);

  Message(long addr) {
    this.ptr = Ptr.register(this.getClass(), addr);
  }

  public dev.toad.msg.owned.Message toOwned() {
    return new dev.toad.msg.owned.Message(this);
  }

  public Optional<InetSocketAddress> addr() {
    if (this.addr.isEmpty()) {
      this.addr = Optional.of(this.addr(this.ptr.addr()));
    }

    return this.addr;
  }

  public Id id() {
    return this.id(this.ptr.addr());
  }

  public Token token() {
    return this.token(this.ptr.addr());
  }

  public Code code() {
    return this.code(this.ptr.addr());
  }

  public Type type() {
    return this.typ(this.ptr.addr());
  }

  public dev.toad.msg.ref.Option[] optionRefs() {
    return this.opts(this.ptr.addr());
  }

  public List<dev.toad.msg.Option> options() {
    return Arrays.asList(this.opts(this.ptr.addr()));
  }

  public byte[] payloadBytes() {
    return this.payload(this.ptr.addr());
  }

  public String payloadString() {
    return new String(this.payload(this.ptr.addr()));
  }

  public byte[] toBytes() {
    return this.toBytes(this.ptr.addr());
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
