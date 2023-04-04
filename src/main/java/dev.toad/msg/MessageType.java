package dev.toad.msg;

public enum MessageType {
  CON(1), NON(2), ACK(3), RESET(4);

  private MessageType(int val) {}

  public String toString() {
    switch(this) {
      case CON: return "CON";
      case NON: return "NON";
      case ACK: return "ACK";
      case RESET: return "RESET";
      default: throw new Error();
    }
  }

  public static MessageType fromString(String s) {
    switch(s.toLowerCase().trim()) {
      case "con": return CON;
      case "non": return NON;
      case "ack": return ACK;
      case "reset": return RESET;
      default: throw new Error();
    }
  }
}
