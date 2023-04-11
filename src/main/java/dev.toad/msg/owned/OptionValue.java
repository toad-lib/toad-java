package dev.toad.msg.owned;

public class OptionValue implements dev.toad.msg.OptionValue {

  public final byte[] bytes;

  public OptionValue(byte[] bytes) {
    this.bytes = bytes;
  }

  public OptionValue(dev.toad.msg.ref.OptionValue ref) {
    this(ref.asBytes().clone());
  }

  public byte[] asBytes() {
    return this.bytes;
  }

  public String asString() {
    return new String(this.asBytes());
  }
}
