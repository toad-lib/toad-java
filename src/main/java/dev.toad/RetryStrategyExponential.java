package dev.toad;

import java.time.Duration;
import java.util.Optional;

  public final class RetryStrategyExponential extends RetryStrategy {

    public final Duration initMin;
    public final Duration initMax;

    private static native RetryStrategyExponential fromRust(byte[] mem);

    private native byte[] toRust();

    public RetryStrategyExponential(Duration initMin, Duration initMax) {
      this.initMin = initMin;
      this.initMax = initMax;
    }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case RetryStrategyExponential e -> e.initMin == this.initMin && e.initMax == this.initMax;
      default -> false;
    };
  }
  }
