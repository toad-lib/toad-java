package dev.toad.msg;

import dev.toad.ffi.Ptr;

public class MessageOptionValueRef
  implements MessageOptionValue, AutoCloseable {

  private final Ptr ptr;

  private native byte[] bytes(long addr);

  public MessageOptionValueRef(long addr) {
    this.ptr = Ptr.register(this.getClass(), addr);
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
    this.ptr.release();
  }
}
