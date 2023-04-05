package dev.toad;

import dev.toad.ffi.*;
import java.time.Duration;
import java.util.Optional;

public final class RetryStrategyExponential extends RetryStrategy {

  public final u64 initMin;
  public final u64 initMax;

  private static native RetryStrategyExponential fromRust(byte[] mem);

  private native byte[] toRust();

  public RetryStrategyExponential(Duration initMin, Duration initMax) {
    if (initMin.isNegative() || initMax.isNegative()) {
      throw new IllegalArgumentException(
        String.format(
          "{initMin: %, initMax: %} neither field may be negative",
          initMin.toMillis(),
          initMax.toMillis()
        )
      );
    }

    this.initMin = new u64(initMin.toMillis());
    this.initMax = new u64(initMax.toMillis());
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case RetryStrategyExponential e -> e.initMin == this.initMin &&
      e.initMax == this.initMax;
      default -> false;
    };
  }
}
