package dev.toad.msg;

import dev.toad.RefHawk;
import dev.toad.RefHawk.Ptr;

public class MessageOptionValueRef
  implements MessageOptionValue, AutoCloseable {

  private final Ptr ptr;

  private native byte[] bytes(long addr);

  public MessageOptionValueRef(long addr) {
    this.ptr = RefHawk.register(this.getClass(), addr);
  }

  public byte[] asBytes() {
    return this.bytes(this.ptr.addr());
  }

  public String asString() {
    return new String(this.bytes(this.ptr.addr()));
  }

  public MessageOptionValue clone() {
    return this;
  }

  @Override
  public void close() {
    RefHawk.release(this.ptr);
  }
}
