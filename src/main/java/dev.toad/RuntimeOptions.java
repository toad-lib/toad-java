package dev.toad;

import dev.toad.ffi.*;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

public final class RuntimeOptions implements Cloneable {

  private Net net;

  public RuntimeOptions() {
    this.net = new Net();
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case RuntimeOptions o -> o.net == this.net;
      default -> false;
    };
  }

  public Net net() {
    return this.net;
  }

  private RuntimeOptions with(Function<RuntimeOptions, RuntimeOptions> f) {
    return f.apply(this.clone());
  }

  public RuntimeOptions withNet(Function<Net, Net> f) {
    return this.with(self -> {
        self.net = f.apply(self.net);
        return self;
      });
  }

  @Override
  public RuntimeOptions clone() {
    RuntimeOptions self = new RuntimeOptions();
    self.net = this.net.clone();
    return self;
  }

  public final class Net implements Cloneable {

    private u16 port;
    private u8 concurrency;
    private Msg msg;

    public Net() {
      this.port = new u16(5683);
      this.concurrency = new u8((short)1);
      this.msg = new Msg();
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case Net o -> o.port == this.port &&
        o.concurrency == this.concurrency &&
        o.msg == this.msg;
        default -> false;
      };
    }

    private Net with(Function<Net, Net> f) {
      return f.apply(this.clone());
    }

    @Override
    public Net clone() {
      Net self = new Net();
      self.port = this.port;
      self.concurrency = this.concurrency;
      self.msg = this.msg.clone();
      return self;
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

    public Net withPort(short port) {
      return this.with(self -> {
          self.port = new u16(port);
          return self;
        });
    }

    public Net withConcurrency(short conc) {
      return this.with(self -> {
          self.concurrency = new u8(conc);
          return self;
        });
    }

    public Net withMsg(Function<Msg, Msg> f) {
      return this.with(self -> {
          self.msg = f.apply(self.msg);
          return self;
        });
    }
  }

  public final class Msg implements Cloneable {

    private Optional<u16> tokenSeed = Optional.empty();
    private Optional<u16> probingRateBytesPerSecond = Optional.empty();
    private Optional<Duration> multicastResponseLeisure = Optional.empty();
    private Con con;
    private Non non;

    public Msg() {
      this.con = new Con();
      this.non = new Non();
    }

    private Msg with(Function<Msg, Msg> f) {
      return f.apply(this.clone());
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

    @Override
    public Msg clone() {
      Msg self = new Msg();
      self.tokenSeed = this.tokenSeed;
      self.probingRateBytesPerSecond = this.probingRateBytesPerSecond;
      self.multicastResponseLeisure = this.multicastResponseLeisure;
      self.con = this.con.clone();
      self.non = this.non.clone();
      return self;
    }

    public Optional<Integer> tokenSeed() {
      return this.tokenSeed.map(u16 -> u16.intValue());
    }

    public Optional<Integer> probingRateBytesPerSecond() {
      return this.probingRateBytesPerSecond.map(u16 -> u16.intValue());
    }

    public Optional<Duration> multicastResponseLeisure() {
      return this.multicastResponseLeisure;
    }

    public Con con() {
      return this.con;
    }

    public Non non() {
      return this.non;
    }

    public Msg withTokenSeed(int tokenSeed) {
      return this.with(self -> {
          self.tokenSeed = Optional.of(new u16(tokenSeed));
          return self;
        });
    }

    public Msg withProbingRateBytesBerSecond(int bps) {
      return this.with(m -> {
          m.probingRateBytesPerSecond = Optional.of(new u16(bps));
          return m;
        });
    }

    public Msg withMulticastResponseLeisure(Duration dur) {
      return this.with(m -> {
          m.multicastResponseLeisure = Optional.of(dur);
          return m;
        });
    }

    public Msg withCon(Function<Con, Con> f) {
      return this.with(m -> {
          m.con = f.apply(m.con);
          return m;
        });
    }

    public Msg withNon(Function<Non, Non> f) {
      return this.with(m -> {
          m.non = f.apply(m.non);
          return m;
        });
    }

    public final class Con implements Cloneable {

      private Optional<RetryStrategy> ackedRetryStrategy = Optional.empty();
      private Optional<RetryStrategy> unackedRetryStrategy = Optional.empty();
      private Optional<u16> maxAttempts = Optional.empty();

      public Con() {}

      private Con with(Function<Con, Con> f) {
        return f.apply(this.clone());
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

      public Optional<RetryStrategy> ackedRetryStrategy() {
        return this.ackedRetryStrategy;
      }

      public Optional<RetryStrategy> unackedRetryStrategy() {
        return this.unackedRetryStrategy;
      }

      public Optional<Integer> maxAttempts() {
        return this.maxAttempts.map(u16 -> u16.intValue());
      }

      public Con withAckedRetryStrategy(RetryStrategy r) {
        return this.with(s -> {
            s.ackedRetryStrategy = Optional.of(r);
            return s;
          });
      }

      public Con withUnackedRetryStrategy(RetryStrategy r) {
        return this.with(s -> {
            s.unackedRetryStrategy = Optional.of(r);
            return s;
          });
      }

      public Con withMaxAttempts(int a) {
        return this.with(s -> {
            s.maxAttempts = Optional.of(new u16(a));
            return s;
          });
      }

      @Override
      public Con clone() {
        return this;
      }
    }

    public final class Non implements Cloneable {

      private Optional<RetryStrategy> retryStrategy = Optional.empty();
      private Optional<u16> maxAttempts = Optional.empty();

      public Non() {}

      @Override
      public boolean equals(Object other) {
        return switch (other) {
          case Non o -> this.retryStrategy == o.retryStrategy &&
          this.maxAttempts == o.maxAttempts;
          default -> false;
        };
      }

      private Non with(Function<Non, Non> f) {
        return f.apply(this.clone());
      }

      public Optional<RetryStrategy> retryStrategy() {
        return this.retryStrategy;
      }

      public Optional<Integer> maxAttempts() {
        return this.maxAttempts.map(u16 -> u16.intValue());
      }

      public Non withRetryStrategy(RetryStrategy r) {
        return this.with(s -> {
            s.retryStrategy = Optional.of(r);
            return s;
          });
      }

      public Non withMaxAttempts(int a) {
        return this.with(s -> {
            s.maxAttempts = Optional.of(new u16(a));
            return s;
          });
      }

      @Override
      public Non clone() {
        return this;
      }
    }
  }
}
