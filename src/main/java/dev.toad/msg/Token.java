package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import java.util.ArrayList;
import java.util.Arrays;

public final class Token implements Debug {

  public static native Token defaultToken();

  public static final Eq<Token> eq = Eq.byteArray.contramap(Token::toBytes);

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
      case Token t -> Token.eq.test(this, t);
      default -> false;
    };
  }
}
