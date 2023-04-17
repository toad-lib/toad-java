package dev.toad.msg.build;

import dev.toad.msg.Code;
import dev.toad.msg.Id;
import dev.toad.msg.Token;
import dev.toad.msg.Type;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Message
  implements MessageNeeds.Code, MessageNeeds.Destination, MessageNeeds.Type {

  HashMap<Long, ArrayList<dev.toad.msg.owned.OptionValue>> options =
    new HashMap<>();
  Optional<Id> id = Optional.empty();
  Optional<Token> token = Optional.empty();
  Optional<InetSocketAddress> addr = Optional.empty();
  Optional<Code> code = Optional.empty();
  Optional<Type> type = Optional.empty();
  byte[] payload = new byte[] {};

  Message() {}

  public static MessageNeeds.Destination builder() {
    return new Message();
  }

  public MessageNeeds.Type addr(InetSocketAddress addr) {
    this.addr = Optional.of(addr);
    return this;
  }

  public MessageNeeds.Code type(Type type) {
    this.type = Optional.of(type);
    return this;
  }

  public Message code(Code code) {
    this.code = Optional.of(code);
    return this;
  }

  public Message id(Id id) {
    this.id = Optional.of(id);
    return this;
  }

  public Message token(Token token) {
    this.token = Optional.of(token);
    return this;
  }

  public Message option(
    Function<OptionNeeds.Number, dev.toad.msg.owned.Option> fun
  ) {
    var opt = fun.apply(Option.builder());
    return this.option(opt);
  }

  public Message option(dev.toad.msg.Option opt) {
    return this.option(opt.number(), opt.values());
  }

  public Message option(long number, List<dev.toad.msg.OptionValue> values) {
    if (this.options.get(number) == null) {
      this.options.put(number, new ArrayList<>());
    }

    this.options.get(number)
      .addAll(values.stream().map(v -> v.toOwned()).toList());
    return this;
  }

  public Message payload(String payload) {
    this.payload = payload.getBytes();
    return this;
  }

  public Message payload(byte[] payload) {
    this.payload = payload;
    return this;
  }

  public dev.toad.msg.Message build() {
    return new dev.toad.msg.owned.Message(
      this.addr,
      this.type.get(),
      this.code.get(),
      this.id.orElse(Id.defaultId()),
      this.token.orElse(Token.defaultToken()),
      this.payload,
      this.options.entrySet()
        .stream()
        .map(ent -> new dev.toad.msg.owned.Option(ent.getKey(), ent.getValue()))
        .collect(Collectors.toCollection(() -> new ArrayList<>()))
    );
  }
}
