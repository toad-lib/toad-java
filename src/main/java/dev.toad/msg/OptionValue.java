package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import java.util.ArrayList;
import java.util.List;

public interface OptionValue extends Debug {
  public byte[] asBytes();

  public String asString();

  public dev.toad.msg.owned.OptionValue toOwned();

  public static Eq<OptionValue> eq() {
    return Eq.byteArray.contramap(OptionValue::asBytes);
  }

  public default boolean equals(OptionValue o) {
    return this.asBytes().equals(o.asBytes());
  }

  @Override
  public default String toDebugString() {
    List<Integer> intList = new ArrayList<>();
    var bytes = this.asBytes();
    for (byte b : bytes) {
      intList.add((int) b);
    }

    return intList.toString();
  }
}
