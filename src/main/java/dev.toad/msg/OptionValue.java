package dev.toad.msg;

public interface OptionValue {
  public byte[] asBytes();

  public String asString();

  public dev.toad.msg.owned.OptionValue toOwned();

  public default boolean equals(OptionValue o) {
    return this.asBytes().equals(o.asBytes());
  }
}
