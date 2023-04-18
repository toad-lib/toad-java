package dev.toad.msg.ref;

import dev.toad.ffi.Ptr;
import dev.toad.msg.*;
import java.util.Arrays;
import java.util.List;

public class Option implements dev.toad.msg.Option, AutoCloseable {

  Ptr ptr;
  final long number;

  public native dev.toad.msg.ref.OptionValue[] valueRefs();

  Option(long addr, long number) {
    this.ptr = Ptr.register(this.getClass(), addr);
    this.number = number;
  }

  public long number() {
    return this.number;
  }

  public List<dev.toad.msg.OptionValue> values() {
    return Arrays.asList(this.valueRefs());
  }

  public dev.toad.msg.owned.Option toOwned() {
    return new dev.toad.msg.owned.Option(this);
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
