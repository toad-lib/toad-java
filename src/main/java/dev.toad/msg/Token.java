package dev.toad.msg;

import dev.toad.Debug;
import java.util.ArrayList;

public final class Token implements Debug {

  public static native Token defaultToken();

  final byte[] bytes;

  public Token(byte[] bytes) {
    this.bytes = bytes;
  }

  public byte[] toBytes() {
    return this.bytes;
  }

  @Override
  public String toDebugString() {
    var intList = new ArrayList<Integer>();
    for (byte b : this.bytes) {
      intList.add((int) b);
    }

    return String.format("Token(%s)", intList);
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Token t -> t.bytes.equals(this.bytes);
      default -> false;
    };
  }
}
