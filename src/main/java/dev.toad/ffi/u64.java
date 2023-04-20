package dev.toad.ffi;

import java.math.BigInteger;

public final class u64 {

  public native byte[] toBytes();

  public static final BigInteger MAX = BigInteger.TWO
    .pow(64)
    .subtract(BigInteger.ONE);
  private final BigInteger l;

  public u64(BigInteger l) {
    uint.assertWithinRange(this.MAX, l);
    this.l = l;
  }

  public u64(long l) {
    this(BigInteger.valueOf(l));
  }

  public BigInteger bigintValue() {
    return this.l;
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case u64 o -> this.equals(o);
      default -> false;
    };
  }

  public boolean equals(u64 other) {
    return this.bigintValue() == other.bigintValue();
  }
}
