package dev.toad.msg.owned;

import java.nio.charset.StandardCharsets;

public class OptionValue implements dev.toad.msg.OptionValue {

  public final byte[] bytes;

  public OptionValue(String str) {
    this.bytes = str.getBytes(StandardCharsets.UTF_8);
  }

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

  public dev.toad.msg.owned.OptionValue toOwned() {
    return this;
  }
}
