package dev.toad.ffi;

public final class u8 {

  public static final short MAX = (short) (Math.pow(2, 8) - 1);
  private final short l;

  public u8(short l) {
    uint.assertWithinRange(this.MAX, l);
    this.l = l;
  }

  public short shortValue() {
    return this.l;
  }
}
