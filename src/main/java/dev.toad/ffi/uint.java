package dev.toad.ffi;

public class uint {
  public static void assertWithinRange(double max, double n) {
    if (n < 0 || n > max) {
      throw new IllegalArgumentException(String.format("% must be between 0 and %", n, max));
    }
  }

  public static void assertNatural(double n) {
    if (n % 1 > 0.0) {
      throw new IllegalArgumentException(String.format("% must be a whole integer", n));
    }
  }
}
