package dev.toad;

import dev.toad.ffi.*;
import dev.toad.msg.MessageRef;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

public class ToadRuntime {

  static native Config defaultConfigImpl();

  static Config defaultConfig = null;

  static Config defaultConfig() {
    if (ToadRuntime.defaultConfig == null) {
      ToadRuntime.defaultConfig = ToadRuntime.defaultConfigImpl();
    }

    return ToadRuntime.defaultConfig;
  }

  static {
    System.loadLibrary("toad_java_glue");
  }

  private final Ptr ptr;
  private final Config config;

  private static native long init(Config o);

  private static native Optional<MessageRef> pollReq(long addr);

  Optional<MessageRef> pollReq() {
    return ToadRuntime.pollReq(this.ptr.addr());
  }

  public static BuilderRequiresBindToAddress builder() {
    return new Builder();
  }

  ToadRuntime(Config o) {
    this.config = o;
    this.ptr = Ptr.register(this.getClass(), this.init(o));
  }

  public Config config() {
    return this.config;
  }

  public interface BuilderRequiresBindToAddress {
    ToadRuntime.Builder port(short port);
    ToadRuntime.Builder address(InetSocketAddress addr);
  }

  public static final class Builder implements BuilderRequiresBindToAddress {

    Config.Msg.Builder msg = Config.Msg.builder();
    Optional<InetSocketAddress> addr = Optional.empty();
    u8 concurrency = ToadRuntime.defaultConfig().concurrency;

    Builder() {}

    public ToadRuntime build() {
      var cfg = new Config(this.addr.get(), this.concurrency, this.msg.build());
      return new ToadRuntime(cfg);
    }

    public Builder msg(Function<Config.Msg.Builder, Config.Msg.Builder> f) {
      this.msg = f.apply(this.msg);
      return this;
    }

    public Builder port(short port) {
      return this.address(new InetSocketAddress(port));
    }

    public Builder address(InetSocketAddress addr) {
      this.addr = Optional.of(addr);
      return this;
    }

    public Builder concurrency(byte concurrency) {
      this.concurrency = new u8(concurrency);
      return this;
    }
  }

  public static final class Config {

    final InetSocketAddress addr;
    final u8 concurrency;
    final Msg msg;

    Config(InetSocketAddress addr, u8 concurrency, Msg msg) {
      this.addr = addr;
      this.concurrency = concurrency;
      this.msg = msg;
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case Config o -> o.addr == this.addr &&
        o.concurrency == this.concurrency &&
        o.msg == this.msg;
        default -> false;
      };
    }

    public InetSocketAddress addr() {
      return this.addr();
    }

    public short concurrency() {
      return this.concurrency.shortValue();
    }

    public Msg msg() {
      return this.msg;
    }

    public static final class Msg {

      u16 tokenSeed;
      u16 probingRateBytesPerSecond;
      Duration multicastResponseLeisure;
      Con con;
      Non non;

      public static Builder builder() {
        return new Builder();
      }

      Msg(
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

        Con.Builder con = Con.builder();
        Non.Builder non = Non.builder();
        u16 tokenSeed = ToadRuntime.defaultConfig().msg.tokenSeed;
        u16 probingRateBytesPerSecond = ToadRuntime.defaultConfig()
          .msg.probingRateBytesPerSecond;
        Duration multicastResponseLeisure = ToadRuntime.defaultConfig()
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

        public Builder con(Function<Con.Builder, Con.Builder> f) {
          this.con = f.apply(this.con);
          return this;
        }

        public Builder non(Function<Non.Builder, Non.Builder> f) {
          this.non = f.apply(this.non);
          return this;
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

        Builder() {}
      }

      public static final class Con {

        final RetryStrategy ackedRetryStrategy;
        final RetryStrategy unackedRetryStrategy;
        final u16 maxAttempts;

        public static Builder builder() {
          return new Builder();
        }

        Con(
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

          RetryStrategy ackedRetryStrategy = ToadRuntime.defaultConfig()
            .msg.con.ackedRetryStrategy;
          RetryStrategy unackedRetryStrategy = ToadRuntime.defaultConfig()
            .msg.con.unackedRetryStrategy;
          u16 maxAttempts = ToadRuntime.defaultConfig().msg.con.maxAttempts;

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

          Builder() {}
        }
      }

      public static final class Non {

        final RetryStrategy retryStrategy;
        final u16 maxAttempts;

        public static Builder builder() {
          return new Builder();
        }

        Non(RetryStrategy retryStrategy, u16 maxAttempts) {
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

          RetryStrategy retryStrategy = ToadRuntime.defaultConfig()
            .msg.non.retryStrategy;
          u16 maxAttempts = ToadRuntime.defaultConfig().msg.non.maxAttempts;

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

          Builder() {}
        }
      }
    }
  }
}
