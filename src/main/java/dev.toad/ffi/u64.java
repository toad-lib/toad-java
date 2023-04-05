package dev.toad.ffi;

public final class u64 {
  public static final double MAX = Math.pow(2, 64) - 1;
  private final double l;

  public u64(double l) {
    uint.assertWithinRange(this.MAX, l);
    uint.assertNatural(l);

    this.l = l;
  }

  public double doubleValue() {
    return this.l;
  }
}
