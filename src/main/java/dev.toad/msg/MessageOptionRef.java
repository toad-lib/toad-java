package dev.toad.msg;

import dev.toad.ffi.Ptr;
import java.util.Arrays;
import java.util.List;

public class MessageOptionRef implements MessageOption, AutoCloseable {

  private Ptr ptr;
  private final long number;

  private native MessageOptionValueRef[] values(long ptr);

  public MessageOptionRef(long addr, long number) {
    this.ptr = Ptr.register(this.getClass(), addr);
    this.number = number;
  }

  public long number() {
    return this.number;
  }

  public MessageOptionValueRef[] valueRefs() {
    return this.values(this.ptr.addr());
  }

  public List<MessageOptionValue> values() {
    return Arrays.asList(this.values(this.ptr.addr()));
  }

  public MessageOption clone() {
    return new MessageOptionOwned(this);
  }

  @Override
  public void close() {
    this.ptr.release();
  }
}
