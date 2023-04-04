package dev.toad;

import java.time.Duration;
import java.util.Optional;

public abstract sealed class RetryStrategy {

  @Override
  public boolean equals(Object other) {
    return switch (this) {
      case Exponential self -> switch (other) {
        case Exponential e -> e.initMin == self.initMin &&
        e.initMax == self.initMax;
        default -> false;
      };
      case Linear self -> switch (other) {
        case Linear e -> e.min == self.min && e.max == self.max;
        default -> false;
      };
      default -> false;
    };
  }

  public final class Exponential extends RetryStrategy {

    public final Duration initMin;
    public final Duration initMax;

    private static native Exponential fromRust(byte[] mem);

    private native byte[] toRust();

    public Exponential(Duration initMin, Duration initMax) {
      this.initMin = initMin;
      this.initMax = initMax;
    }
  }

  public final class Linear extends RetryStrategy {

    public final Duration min;
    public final Duration max;

    private static native Linear fromRust(byte[] mem);

    private native byte[] toRust();

    public Linear(Duration min, Duration max) {
      this.min = min;
      this.max = max;
    }
  }
}
