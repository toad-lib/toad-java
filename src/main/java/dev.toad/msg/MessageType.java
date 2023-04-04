package dev.toad.msg;

public enum MessageType {
  CON(1),
  NON(2),
  ACK(3),
  RESET(4);

  private MessageType(int val) {}

  public String toString() {
    return switch (this) {
      case CON -> "CON";
      case NON -> "NON";
      case ACK -> "ACK";
      case RESET -> "RESET";
      default -> throw new Error();
    };
  }

  public static MessageType fromString(String s) {
    return switch (s.toLowerCase().trim()) {
      case "con" -> CON;
      case "non" -> NON;
      case "ack" -> ACK;
      case "reset" -> RESET;
      default -> throw new Error();
    };
  }
}
