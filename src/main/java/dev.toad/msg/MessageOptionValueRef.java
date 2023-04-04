package dev.toad.msg;

public class MessageOptionValueRef implements MessageOptionValue {

  private final long addr;

  private native byte[] bytes(long addr);

  public MessageOptionValueRef(long addr) {
    this.addr = addr;
  }

  public byte[] asBytes() {
    return this.bytes(this.addr);
  }

  public String asString() {
    return new String(this.bytes(this.addr));
  }

  public MessageOptionValue clone() {
    return this;
  }
}
