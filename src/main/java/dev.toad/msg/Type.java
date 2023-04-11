package dev.toad.msg;

public enum Type {
  CON(1),
  NON(2),
  ACK(3),
  RESET(4);

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
}
