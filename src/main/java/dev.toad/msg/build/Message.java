package dev.toad.msg.build;

import dev.toad.msg.Code;
import dev.toad.msg.Id;
import dev.toad.msg.Payload;
import dev.toad.msg.Token;
import dev.toad.msg.Type;
import dev.toad.msg.option.Host;
import dev.toad.msg.option.Observe;
import dev.toad.msg.option.Path;
import dev.toad.msg.option.Query;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
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
  Optional<Payload> payload = Optional.empty();

  Message() {}

  public static MessageNeeds.Code respondTo(dev.toad.msg.Message other) {
    return Message.respondTo(other, false);
  }

  public static MessageNeeds.Code respondTo(
    dev.toad.msg.Message other,
    boolean conResponseToNonRequest
  ) {
    // prettier-ignore
    Type type = Type.eq.test(other.type(), Type.CON) ? Type.ACK
              : conResponseToNonRequest              ? Type.CON
              : Type.NON;

    return Message.copyOf(other).unsetId().unsetOption(Observe.number).type(type);
  }

  public static Message copyOf(dev.toad.msg.Message other) {
    var builder = new Message();

    Function<dev.toad.msg.Option, Long> key;
    Function<dev.toad.msg.Option, ArrayList<dev.toad.msg.owned.OptionValue>> value;
    BinaryOperator<ArrayList<dev.toad.msg.owned.OptionValue>> merge;

    key = o -> o.number();
    value =
      o ->
        o
          .values()
          .stream()
          .map(dev.toad.msg.OptionValue::toOwned)
          .collect(Collectors.toCollection(ArrayList::new));
    merge =
      (a, b) -> {
        a.addAll(b);
        return a;
      };

    builder.options =
      other
        .options()
        .stream()
        .collect(Collectors.toMap(key, value, merge, HashMap::new));
    builder.id = Optional.of(other.id());
    builder.code = Optional.of(other.code());
    builder.token = Optional.of(other.token());
    builder.type = Optional.of(other.type());
    builder.payload = Optional.of(other.payload());
    builder.addr = other.addr();

    return builder;
  }

  public static MessageNeeds.Destination builder() {
    return new Message();
  }

  public Message uri(String uriStr)
    throws URISyntaxException, UnknownHostException {
    var uri = new URI(uriStr);
    var addr = InetAddress.getByName(uri.getHost());
    var secure = uri.getScheme() != null && uri.getScheme().equals("coaps");

    // prettier-ignore
    var port = uri.getPort() > 0 ? uri.getPort()
             : secure ? 5684
             : 5683;

    this.addr = Optional.of(new InetSocketAddress(addr, port));

    this.option(new Host(addr.getHostAddress()));

    if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
      this.option(new Query(uri.getQuery()));
    }

    if (uri.getPath() != null && !uri.getPath().isEmpty()) {
      this.option(new Path(uri.getPath()));
    }

    return this;
  }

  public Message addr(InetSocketAddress addr) {
    this.addr = Optional.of(addr);
    return this;
  }

  public Message type(Type type) {
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

  public Message unsetId() {
    this.id = Optional.empty();
    return this;
  }

  public Message token(Token token) {
    this.token = Optional.of(token);
    return this;
  }

  public Message unsetToken() {
    this.token = Optional.empty();
    return this;
  }

  public Message unsetOption(long num) {
    this.options.remove(num);
    return this;
  }

  public Message payload(Payload payload) {
    this.payload = Optional.of(payload);
    if (!payload.contentFormat().isEmpty()) {
      this.option(payload.contentFormat().get());
    }
    return this;
  }

  public Message option(
    Function<OptionNeeds.Number, dev.toad.msg.owned.Option> fun
  ) {
    var opt = fun.apply(Option.builder());
    return this.option(opt);
  }

  public Message option(dev.toad.msg.Option opt) {
    return this.putOption(opt.number(), opt.values());
  }

  public Message putOption(long number, List<dev.toad.msg.OptionValue> values) {
    this.options.put(
        number,
        values
          .stream()
          .map(v -> v.toOwned())
          .collect(Collectors.toCollection(ArrayList::new))
      );
    return this;
  }

  public Message option(long number, List<dev.toad.msg.OptionValue> values) {
    var vals = Optional
      .ofNullable(this.options.get(number))
      .orElse(new ArrayList<>());
    vals.addAll(values.stream().map(v -> v.toOwned()).toList());
    return this.putOption(number, List.copyOf(vals));
  }

  public dev.toad.msg.Message build() {
    return new dev.toad.msg.owned.Message(
      this.addr,
      this.type.get(),
      this.code.get(),
      this.id.orElse(Id.defaultId()),
      this.token.orElse(Token.defaultToken()),
      this.payload.orElse(new Payload()),
      this.options.entrySet()
        .stream()
        .map(ent -> new dev.toad.msg.owned.Option(ent.getKey(), ent.getValue()))
        .collect(Collectors.toCollection(() -> new ArrayList<>()))
    );
  }
}
