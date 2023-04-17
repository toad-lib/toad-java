package dev.toad.ffi;

public final class u16 {

  public native byte[] toBytes();

  public static final int MAX = (int) (Math.pow(2, 16) - 1);
  private final int l;

  public u16(int l) {
    uint.assertWithinRange(this.MAX, l);
    this.l = l;
  }

  public int intValue() {
    return this.l;
  }
}
