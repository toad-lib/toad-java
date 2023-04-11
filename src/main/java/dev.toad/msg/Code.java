package dev.toad.msg;

public class Code {

  private final int clazz;
  private final int detail;

  public Code(int clazz, int detail) {
    this.clazz = clazz;
    this.detail = detail;
  }

  @Override
  public String toString() {
    if (this.isRequest()) {
      switch (this.detail) {
        case 1:
          return "GET";
        case 2:
          return "PUT";
        case 3:
          return "POST";
        case 4:
          return "DELETE";
        default:
          throw new Error();
      }
    } else {
      return String.format("%d.%d", this.clazz, this.detail);
    }
  }

  public boolean isRequest() {
    return this.clazz == 0 && this.detail > 0;
  }

  public boolean isResponse() {
    return this.clazz > 1;
  }

  public boolean isEmpty() {
    return this.clazz == 0 && this.detail == 0;
  }
}
