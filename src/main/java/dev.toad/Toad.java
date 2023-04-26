package dev.toad;

import dev.toad.ffi.*;
import dev.toad.msg.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class Toad implements AutoCloseable {

  static native Config defaultConfigImpl();

  static Config defaultConfig = null;

  static Config defaultConfig() {
    if (Toad.defaultConfig == null) {
      Toad.defaultConfig = Toad.defaultConfigImpl();
    }

    return Toad.defaultConfig;
  }

  static {
    Toad.loadNativeLib();
  }

  public static void loadNativeLib() {
    System.loadLibrary("toad_java_glue");
  }

  final Logger logger;
  final Ptr ptr;
  final Config config;
  final DatagramChannel channel;

  static native void teardown();

  static native long init(Logger logger, DatagramChannel chan, Config o);

  static native Optional<IdAndToken> sendMessage(
    long ptr,
    dev.toad.msg.owned.Message msg
  );

  native void notify(String path);

  static native Optional<dev.toad.msg.ref.Message> pollReq(long ptr);

  static native Optional<dev.toad.msg.ref.Message> pollResp(
    long ptr,
    dev.toad.msg.Token t,
    InetSocketAddress n
  );

  public Optional<IdAndToken> sendMessage(Message msg) {
    return Toad.sendMessage(this.ptr.addr(), msg.toOwned());
  }

  public Optional<dev.toad.msg.ref.Message> pollReq() {
    return Toad.pollReq(this.ptr.addr());
  }

  public Optional<dev.toad.msg.ref.Message> pollResp(
    Token regarding,
    InetSocketAddress from
  ) {
    return Toad.pollResp(this.ptr.addr(), regarding, from);
  }

  public static BuilderRequiresSocket builder() {
    return new Builder();
  }

  Toad(Config o, DatagramChannel channel) {
    this.logger = Logger.getLogger("dev.toad");
    this.config = o;
    this.channel = channel;
    this.ptr =
      Ptr.register(this.getClass(), this.init(this.logger, this.channel, o));
  }

  Toad(Config o, Logger logger, DatagramChannel channel) {
    this.logger = logger;
    this.config = o;
    this.channel = channel;
    this.ptr =
      Ptr.register(this.getClass(), this.init(this.logger, this.channel, o));
  }

  public DatagramChannel channel() {
    return this.channel;
  }

  public Config config() {
    return this.config;
  }

  public Logger logger() {
    return this.logger;
  }

  public InetSocketAddress localAddress() throws IOException {
    return (InetSocketAddress) this.channel.getLocalAddress();
  }

  @Override
  public void close() {
    Toad.teardown();

    try {
      this.channel.close();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

    this.ptr.release();
  }

  public static final class IdAndToken {

    public final Id id;
    public final Token token;

    public IdAndToken(Id id, Token token) {
      this.id = id;
      this.token = token;
    }
  }

  static final class LogMessage {

    static String rx(Message msg) {
      return String.format(
        "<== %s\n%s",
        msg.addr().get().toString(),
        msg.toDebugString()
      );
    }

    static String tx(Message msg) {
      return String.format(
        "==> %s\n%s",
        msg.addr().get().toString(),
        msg.toDebugString()
      );
    }
  }

  public interface BuilderRequiresSocket {
    Toad.Builder port(short port);
    Toad.Builder address(InetSocketAddress addr);
    Toad.Builder channel(DatagramChannel channel);
  }

  public static final class Builder implements BuilderRequiresSocket {

    Optional<IOException> ioException = Optional.empty();
    Config.Msg.Builder msg = Config.Msg.builder();
    Optional<DatagramChannel> channel = Optional.empty();
    Optional<java.util.logging.Level> logLevel = Optional.empty();
    Optional<java.util.logging.Formatter> logFormatter = Optional.empty();
    Optional<String> loggerName = Optional.empty();
    u8 concurrency = Toad.defaultConfig().concurrency;

    Builder() {}

    Logger logger() {
      var logger = Logger.getLogger(this.loggerName.orElse("dev.toad"));
      if (logger.getUseParentHandlers()) {
        var level = this.logLevel.orElse(Level.INFO);

        var handler = new ConsoleHandler();
        handler.setFormatter(this.logFormatter.orElse(new SimpleFormatter()));
        handler.setLevel(level);

        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(level);
      }

      return logger;
    }

    public Client buildClient() throws IOException {
      if (this.ioException.isEmpty()) {
        var cfg = new Config(this.concurrency, this.msg.build());
        var toad = new Toad(cfg, this.logger(), this.channel.get());
        return new Client(toad);
      } else {
        throw this.ioException.get();
      }
    }

    public Server.Builder server() throws IOException {
      if (this.ioException.isEmpty()) {
        var cfg = new Config(this.concurrency, this.msg.build());
        var toad = new Toad(cfg, this.logger(), this.channel.get());
        return new Server.Builder(toad);
      } else {
        throw this.ioException.get();
      }
    }

    public Builder msg(Function<Config.Msg.Builder, Config.Msg.Builder> f) {
      this.msg = f.apply(this.msg);
      return this;
    }

    public Builder port(short port) {
      return this.address(new InetSocketAddress(port));
    }

    public Builder logLevel(java.util.logging.Level level) {
      this.logLevel = Optional.of(level);
      return this;
    }

    public Builder logFormatter(java.util.logging.Formatter f) {
      this.logFormatter = Optional.of(f);
      return this;
    }

    public Builder loggerName(String name) {
      this.loggerName = Optional.of(name);
      return this;
    }

    public Builder address(InetSocketAddress addr) {
      try {
        DatagramChannel channel = DatagramChannel.open(
          java.net.StandardProtocolFamily.INET
        );
        channel.bind(addr);
        channel.configureBlocking(false);
        return this.channel(channel);
      } catch (java.io.IOException e) {
        this.ioException = Optional.of(e);
        return this;
      }
    }

    public Builder channel(DatagramChannel channel) {
      this.channel = Optional.of(channel);
      return this;
    }

    public Builder concurrency(byte concurrency) {
      this.concurrency = new u8(concurrency);
      return this;
    }
  }

  public static final class Config {

    final u8 concurrency;
    final Msg msg;

    Config(u8 concurrency, Msg msg) {
      this.concurrency = concurrency;
      this.msg = msg;
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case Config o -> o.concurrency.equals(this.concurrency) &&
        o.msg.equals(this.msg);
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
          case Msg o -> o.tokenSeed.equals(this.tokenSeed) &&
          o.probingRateBytesPerSecond.equals(this.probingRateBytesPerSecond) &&
          o.multicastResponseLeisure.equals(this.multicastResponseLeisure) &&
          o.con.equals(this.con) &&
          o.non.equals(this.non);
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
        u16 tokenSeed = Toad.defaultConfig().msg.tokenSeed;
        u16 probingRateBytesPerSecond = Toad.defaultConfig()
          .msg.probingRateBytesPerSecond;
        Duration multicastResponseLeisure = Toad.defaultConfig()
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
            case Con o -> this.ackedRetryStrategy.equals(
                o.ackedRetryStrategy
              ) &&
            this.unackedRetryStrategy.equals(o.unackedRetryStrategy) &&
            this.maxAttempts.equals(o.maxAttempts);
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

          RetryStrategy ackedRetryStrategy = Toad.defaultConfig()
            .msg.con.ackedRetryStrategy;
          RetryStrategy unackedRetryStrategy = Toad.defaultConfig()
            .msg.con.unackedRetryStrategy;
          u16 maxAttempts = Toad.defaultConfig().msg.con.maxAttempts;

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
            case Non o -> this.retryStrategy.equals(o.retryStrategy) &&
            this.maxAttempts.equals(o.maxAttempts);
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

          RetryStrategy retryStrategy = Toad.defaultConfig()
            .msg.non.retryStrategy;
          u16 maxAttempts = Toad.defaultConfig().msg.non.maxAttempts;

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
