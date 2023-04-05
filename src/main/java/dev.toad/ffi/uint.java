package dev.toad.ffi;

import java.math.BigInteger;

public class uint {

  public static void assertWithinRange(long max, long n) {
    uint.assertWithinRange(BigInteger.valueOf(max), BigInteger.valueOf(n));
  }

  public static void assertWithinRange(BigInteger max, BigInteger n) {
    if (n.compareTo(BigInteger.ZERO) < 0 || n.compareTo(max) > 0) {
      throw new IllegalArgumentException(
        n.toString() + " must be between 0 and " + max.toString()
      );
    }
  }
}
