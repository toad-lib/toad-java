package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;

public enum Type implements Debug {
  CON(1),
  NON(2),
  ACK(3),
  RESET(4);

  public static final Eq<Type> eq = new Eq<>((a, b) -> a == b);

  private Type(int val) {}

  public String toString() {
    return switch (this) {
      case CON -> "CON";
      case NON -> "NON";
      case ACK -> "ACK";
      case RESET -> "RESET";
      default -> throw new Error();
    };
  }

  public static Type fromString(String s) {
    return switch (s.toLowerCase().trim()) {
      case "con" -> CON;
      case "non" -> NON;
      case "ack" -> ACK;
      case "reset" -> RESET;
      default -> throw new Error();
    };
  }

  @Override
  public String toDebugString() {
    return this.toString();
  }
}
