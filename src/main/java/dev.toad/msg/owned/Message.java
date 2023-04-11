package dev.toad.msg.owned;

import dev.toad.msg.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Message implements dev.toad.msg.Message {

  final InetSocketAddress source;
  final Id id;
  final Token token;
  final byte[] payload;
  final Code code;
  final Type type;
  final List<dev.toad.msg.Option> opts;

  public Message(
    InetSocketAddress source,
    Type type,
    Code code,
    Id id,
    Token token,
    byte[] payload,
    List<dev.toad.msg.Option> opts
  ) {
    this.source = source;
    this.id = id;
    this.token = token;
    this.payload = payload;
    this.code = code;
    this.type = type;
    this.opts = opts;
  }

  public Message(dev.toad.msg.ref.Message ref) {
    this(
      ref.source(),
      ref.type(),
      ref.code(),
      ref.id(),
      ref.token(),
      ref.payloadBytes().clone(),
      Arrays
        .asList(ref.optionRefs())
        .stream()
        .map(dev.toad.msg.ref.Option::clone)
        .collect(Collectors.toList())
    );
  }

  public InetSocketAddress source() {
    return this.source;
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
    return this.opts;
  }

  public byte[] payloadBytes() {
    return this.payload;
  }

  public String payloadString() {
    return new String(this.payload);
  }
}
