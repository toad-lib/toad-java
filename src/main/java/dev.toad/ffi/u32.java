package dev.toad.ffi;

public final class u32 {

  public native byte[] toBytes();

  public static final long MAX = (long) (Math.pow(2, 32) - 1);
  private final long l;

  public u32(long l) {
    uint.assertWithinRange(this.MAX, l);
    this.l = l;
  }

  public long longValue() {
    return this.l;
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case u32 o -> this.equals(o);
      default -> false;
    };
  }

  public boolean equals(u32 other) {
    return this.longValue() == other.longValue();
  }
}
