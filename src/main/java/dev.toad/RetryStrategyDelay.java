package dev.toad;

import dev.toad.ffi.*;
import java.time.Duration;
import java.util.Optional;

public final class RetryStrategyDelay extends RetryStrategy {

  public final u64 min;
  public final u64 max;

  private static native RetryStrategyDelay fromRust(byte[] mem);

  private native byte[] toRust();

  public RetryStrategyDelay(Duration min, Duration max) {
    if (min.isNegative() || max.isNegative()) {
      throw new IllegalArgumentException(
        String.format(
          "{min: %, max: %} neither field may be negative",
          min.toMillis(),
          max.toMillis()
        )
      );
    }

    this.min = new u64(min.toMillis());
    this.max = new u64(max.toMillis());
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case RetryStrategyDelay e -> e.min == this.min && e.max == this.max;
      default -> false;
    };
  }
}
