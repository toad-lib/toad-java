package dev.toad.msg.ref;

import dev.toad.ffi.Ptr;

public final class OptionValue
  implements dev.toad.msg.OptionValue, AutoCloseable {

  private final Ptr ptr;

  private native byte[] bytes(long addr);

  OptionValue(long addr) {
    this.ptr = Ptr.register(this.getClass(), addr);
  }

  public byte[] asBytes() {
    return this.bytes(this.ptr.addr());
  }

  public String asString() {
    return new String(this.bytes(this.ptr.addr()));
  }

  public dev.toad.msg.owned.OptionValue toOwned() {
    return new dev.toad.msg.owned.OptionValue(this);
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
