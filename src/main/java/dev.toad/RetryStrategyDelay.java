package dev.toad;

import java.time.Duration;
import java.util.Optional;

public final class RetryStrategyDelay extends RetryStrategy {

  public final Duration min;
  public final Duration max;

  private static native RetryStrategyDelay fromRust(byte[] mem);

  private native byte[] toRust();

  public RetryStrategyDelay(Duration min, Duration max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case RetryStrategyDelay e -> e.min == this.min && e.max == this.max;
      default -> false;
    };
  }
}
