package dev.toad.msg;

import dev.toad.ffi.u8;

public class Code {

  final u8 clazz;
  final u8 detail;

  public Code(short clazz, short detail) {
    this.clazz = new u8(clazz);
    this.detail = new u8(detail);
  }

  public short codeClass() {
    return this.clazz.shortValue();
  }

  public short codeDetail() {
    return this.detail.shortValue();
  }

  @Override
  public String toString() {
    if (this.isRequest()) {
      return switch ((Short) this.detail.shortValue()) {
        case 1 -> "GET";
        case 2 -> "PUT";
        case 3 -> "POST";
        case Short other -> "DELETE";
      };
    } else {
      return String.format(
        "%d.%d",
        this.clazz.shortValue(),
        this.detail.shortValue()
      );
    }
  }

  public boolean isRequest() {
    return this.codeClass() == 0 && this.codeDetail() > 0;
  }

  public boolean isResponse() {
    return this.codeClass() > 1;
  }

  public boolean isEmpty() {
    return this.codeClass() == 0 && this.codeDetail() == 0;
  }
}
