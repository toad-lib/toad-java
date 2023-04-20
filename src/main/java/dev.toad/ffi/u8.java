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

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case u8 o -> this.equals(o);
      default -> false;
    };
  }

  public boolean equals(u8 other) {
    return this.shortValue() == other.shortValue();
  }
}
