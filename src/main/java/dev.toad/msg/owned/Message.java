package dev.toad.msg.owned;

import dev.toad.msg.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Message implements dev.toad.msg.Message {

  public native byte[] toBytes();

  final Optional<InetSocketAddress> addr;
  final Id id;
  final Token token;
  final Payload payload;
  final Code code;
  final Type type;
  final ArrayList<dev.toad.msg.owned.Option> opts;

  public Message(
    Optional<InetSocketAddress> addr,
    Type type,
    Code code,
    Id id,
    Token token,
    Payload payload,
    ArrayList<dev.toad.msg.owned.Option> opts
  ) {
    this.addr = addr;
    this.id = id;
    this.token = token;
    this.payload = payload;
    this.code = code;
    this.type = type;
    this.opts = opts;
  }

  public Message(dev.toad.msg.ref.Message ref) {
    this(
      ref.addr(),
      ref.type(),
      ref.code(),
      ref.id(),
      ref.token(),
      ref.payload(),
      Arrays
        .asList(ref.optionRefs())
        .stream()
        .map(dev.toad.msg.ref.Option::toOwned)
        .collect(Collectors.toCollection(() -> new ArrayList<>()))
    );
  }

  public Message toOwned() {
    return this;
  }

  public Optional<InetSocketAddress> addr() {
    return this.addr;
  }

  public Id id() {
    return this.id;
  }

  public Token token() {
    return this.token;
  }

  public Code code() {
    return this.code;
  }

  public Type type() {
    return this.type;
  }

  public List<dev.toad.msg.Option> options() {
    return List.copyOf(this.opts);
  }

  public Payload payload() {
    return this.payload;
  }

  public boolean equals(Object other) {
    return switch (other) {
      case dev.toad.msg.Message m -> m.equals(this);
      default -> false;
    };
  }
}
