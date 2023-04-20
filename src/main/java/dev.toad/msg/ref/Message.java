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

  public native Optional<InetSocketAddress> addr();

  public native Id id();

  public native Token token();

  public native Payload payload();

  public native Code code();

  public native Type type();

  public native dev.toad.msg.ref.Option[] optionRefs();

  public native byte[] toBytes();

  Message(long addr) {
    this.ptr = Ptr.register(this.getClass(), addr);
  }

  public dev.toad.msg.owned.Message toOwned() {
    return new dev.toad.msg.owned.Message(this);
  }

  public List<dev.toad.msg.Option> options() {
    return Arrays.asList(this.optionRefs());
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
