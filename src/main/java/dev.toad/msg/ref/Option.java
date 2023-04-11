package dev.toad.msg.ref;

import dev.toad.ffi.Ptr;
import dev.toad.msg.*;
import java.util.Arrays;
import java.util.List;

public class Option implements dev.toad.msg.Option, AutoCloseable {

  Ptr ptr;
  final long number;

  native dev.toad.msg.ref.OptionValue[] values(long ptr);

  Option(long addr, long number) {
    this.ptr = Ptr.register(this.getClass(), addr);
    this.number = number;
  }

  public long number() {
    return this.number;
  }

  public dev.toad.msg.ref.OptionValue[] valueRefs() {
    return this.values(this.ptr.addr());
  }

  public List<dev.toad.msg.OptionValue> values() {
    return Arrays.asList(this.values(this.ptr.addr()));
  }

  public dev.toad.msg.Option clone() {
    return new dev.toad.msg.owned.Option(this);
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
