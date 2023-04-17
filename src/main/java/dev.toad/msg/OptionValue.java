package dev.toad.msg;

public interface OptionValue {
  public byte[] asBytes();

  public String asString();

  public dev.toad.msg.owned.OptionValue toOwned();
}
