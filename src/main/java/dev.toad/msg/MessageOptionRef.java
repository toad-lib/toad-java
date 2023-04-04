package dev.toad.msg;

import java.util.Arrays;
import java.util.List;

public class MessageOptionRef implements MessageOption {

  private final long addr;
  private final long number;

  private native MessageOptionValueRef[] values(long addr);

  public MessageOptionRef(long addr, long number) {
    this.addr = addr;
    this.number = number;
  }

  public long number() {
    return this.number;
  }

  public MessageOptionValueRef[] valueRefs() {
    return this.values(this.addr);
  }

  public List<MessageOptionValue> values() {
    return Arrays.asList(this.values(this.addr));
  }

  public MessageOption clone() {
    return new MessageOptionOwned(this);
  }
}
