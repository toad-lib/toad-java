package dev.toad;

import dev.toad.ffi.*;
import dev.toad.msg.MessageRef;
import java.time.Duration;
import java.util.Optional;

interface BuilderPort {
  Runtime.Config.Builder port(short port);
}

public class Runtime {

  protected static native Config defaultConfigImpl();

  protected static Config defaultConfig = null;

  protected static Config defaultConfig() {
    if (Runtime.defaultConfig == null) {
      Runtime.defaultConfig = Runtime.defaultConfigImpl();
    }

    return Runtime.defaultConfig;
  }

  static {
    System.loadLibrary("toad_java_glue");
  }

  private final long addr;

  private static native long init(Config o);

  private native Optional<MessageRef> pollReq();

  public static Runtime getOrInit(Config o) {
    return new Runtime(o);
  }

  public Runtime(Config o) {
    this.addr = Runtime.init(o);
  }

  public static final class Config {

    protected u16 port;
    protected u8 concurrency;
    protected Msg msg;

    protected Config(u16 port, u8 concurrency, Msg msg) {
      this.port = port;
      this.concurrency = concurrency;
      this.msg = msg;
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case Config o -> o.port == this.port &&
        o.concurrency == this.concurrency &&
        o.msg == this.msg;
        default -> false;
      };
    }

    public int port() {
      return this.port.intValue();
    }

    public short concurrency() {
      return this.concurrency.shortValue();
    }

    public Msg msg() {
      return this.msg;
    }

    public static final class Builder implements BuilderPort {

      public final Msg.Builder msg = Msg.builder();

      protected Optional<u16> port = Optional.empty();
      protected u8 concurrency = Runtime.defaultConfig().concurrency;

      protected Builder() {}

      public Config build() {
        return new Config(this.port.get(), this.concurrency, this.msg.build());
      }

      public Builder port(short port) {
        this.port = Optional.of(new u16(port));
        return this;
      }

      public Builder concurrency(byte concurrency) {
        this.concurrency = new u8(concurrency);
        return this;
      }
    }

    public static final class Msg {

      protected u16 tokenSeed;
      protected u16 probingRateBytesPerSecond;
      protected Duration multicastResponseLeisure;
      protected Con con;
      protected Non non;

      public static Builder builder() {
        return new Builder();
      }

      protected Msg(
        u16 tokenSeed,
        u16 probingRateBytesPerSecond,
        Duration multicastResponseLeisure,
        Con con,
        Non non
      ) {
        this.tokenSeed = tokenSeed;
        this.probingRateBytesPerSecond = probingRateBytesPerSecond;
        this.multicastResponseLeisure = multicastResponseLeisure;
        this.con = con;
        this.non = non;
      }

      @Override
      public boolean equals(Object other) {
        return switch (other) {
          case Msg o -> o.tokenSeed == this.tokenSeed &&
          o.probingRateBytesPerSecond == this.probingRateBytesPerSecond &&
          o.multicastResponseLeisure == this.multicastResponseLeisure &&
          o.con == this.con &&
          o.non == this.non;
          default -> false;
        };
      }

      public int tokenSeed() {
        return this.tokenSeed.intValue();
      }

      public int probingRateBytesPerSecond() {
        return this.probingRateBytesPerSecond.intValue();
      }

      public Duration multicastResponseLeisure() {
        return this.multicastResponseLeisure;
      }

      public Con con() {
        return this.con;
      }

      public Non non() {
        return this.non;
      }

      public static final class Builder {

        public final Con.Builder con = Con.builder();
        public final Non.Builder non = Non.builder();

        protected u16 tokenSeed = Runtime.defaultConfig().msg.tokenSeed;
        protected u16 probingRateBytesPerSecond = Runtime.defaultConfig()
          .msg.probingRateBytesPerSecond;
        protected Duration multicastResponseLeisure = Runtime.defaultConfig()
          .msg.multicastResponseLeisure;

        public Msg build() {
          return new Msg(
            this.tokenSeed,
            this.probingRateBytesPerSecond,
            this.multicastResponseLeisure,
            this.con.build(),
            this.non.build()
          );
        }

        public Builder tokenSeed(u16 tokenSeed) {
          this.tokenSeed = tokenSeed;
          return this;
        }

        public Builder probingRateBytesPerSecond(
          u16 probingRateBytesPerSecond
        ) {
          this.probingRateBytesPerSecond = probingRateBytesPerSecond;
          return this;
        }

        public Builder multicastResponseLeisure(
          Duration multicastResponseLeisure
        ) {
          this.multicastResponseLeisure = multicastResponseLeisure;
          return this;
        }

        protected Builder() {}
      }

      public static final class Con {

        protected RetryStrategy ackedRetryStrategy;
        protected RetryStrategy unackedRetryStrategy;
        protected u16 maxAttempts;

        public static Builder builder() {
          return new Builder();
        }

        protected Con(
          RetryStrategy unackedRetryStrategy,
          RetryStrategy ackedRetryStrategy,
          u16 maxAttempts
        ) {
          this.unackedRetryStrategy = unackedRetryStrategy;
          this.ackedRetryStrategy = ackedRetryStrategy;
          this.maxAttempts = maxAttempts;
        }

        @Override
        public boolean equals(Object other) {
          return switch (other) {
            case Con o -> this.ackedRetryStrategy == o.ackedRetryStrategy &&
            this.unackedRetryStrategy == o.unackedRetryStrategy &&
            this.maxAttempts == o.maxAttempts;
            default -> false;
          };
        }

        public RetryStrategy ackedRetryStrategy() {
          return this.ackedRetryStrategy;
        }

        public RetryStrategy unackedRetryStrategy() {
          return this.unackedRetryStrategy;
        }

        public int maxAttempts() {
          return this.maxAttempts.intValue();
        }

        public static final class Builder {

          protected RetryStrategy ackedRetryStrategy = Runtime.defaultConfig()
            .msg.con.ackedRetryStrategy;
          protected RetryStrategy unackedRetryStrategy = Runtime.defaultConfig()
            .msg.con.unackedRetryStrategy;
          protected u16 maxAttempts = Runtime.defaultConfig()
            .msg.con.maxAttempts;

          public Con build() {
            return new Con(
              this.unackedRetryStrategy,
              this.ackedRetryStrategy,
              this.maxAttempts
            );
          }

          public Builder ackedRetryStrategy(RetryStrategy ackedRetryStrategy) {
            this.ackedRetryStrategy = ackedRetryStrategy;
            return this;
          }

          public Builder unackedRetryStrategy(
            RetryStrategy unackedRetryStrategy
          ) {
            this.unackedRetryStrategy = unackedRetryStrategy;
            return this;
          }

          public Builder maxAttempts(u16 maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
          }

          protected Builder() {}
        }
      }

      public static final class Non {

        protected RetryStrategy retryStrategy;
        protected u16 maxAttempts;

        public static Builder builder() {
          return new Builder();
        }

        protected Non(RetryStrategy retryStrategy, u16 maxAttempts) {
          this.retryStrategy = retryStrategy;
          this.maxAttempts = maxAttempts;
        }

        @Override
        public boolean equals(Object other) {
          return switch (other) {
            case Non o -> this.retryStrategy == o.retryStrategy &&
            this.maxAttempts == o.maxAttempts;
            default -> false;
          };
        }

        public RetryStrategy retryStrategy() {
          return this.retryStrategy;
        }

        public int maxAttempts() {
          return this.maxAttempts.intValue();
        }

        public static final class Builder {

          protected RetryStrategy retryStrategy = Runtime.defaultConfig()
            .msg.non.retryStrategy;
          protected u16 maxAttempts = Runtime.defaultConfig()
            .msg.non.maxAttempts;

          public Non build() {
            return new Non(this.retryStrategy, this.maxAttempts);
          }

          public Builder retryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
          }

          public Builder maxAttempts(u16 maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
          }

          protected Builder() {}
        }
      }
    }
  }
}
