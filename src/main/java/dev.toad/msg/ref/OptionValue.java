package dev.toad.msg.ref;

import dev.toad.ffi.Ptr;

public final class OptionValue
  implements dev.toad.msg.OptionValue, AutoCloseable {

  final Ptr ptr;

  public native byte[] asBytes();

  OptionValue(long addr) {
    this.ptr = Ptr.register(this.getClass(), addr);
  }

  public String asString() {
    return new String(this.asBytes());
  }

  public dev.toad.msg.owned.OptionValue toOwned() {
    return new dev.toad.msg.owned.OptionValue(this);
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
