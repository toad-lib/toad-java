package dev.toad.msg;

public final class Token {

  final byte[] bytes;

  public Token(byte[] bytes) {
    this.bytes = bytes;
  }

  public byte[] toBytes() {
    return this.bytes;
  }
}
