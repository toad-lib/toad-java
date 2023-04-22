package dev.toad.msg.option;

import dev.toad.Eq;
import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Observe implements Option {

  final boolean register;

  public static final Eq<Observe> eq = Eq.bool.contramap(Observe::isRegister);

  public static final Observe REGISTER = new Observe(true);
  public static final Observe DEREGISTER = new Observe(false);

  public static final long number = 6;

  Observe(boolean register) {
    this.register = register;
  }

  public Observe(Option o) {
    if (o.number() != Observe.number) {
      throw new IllegalArgumentException(
        String.format("%d != Observe number %d", o.number(), Path.number)
      );
    }

    if (o.values().size() > 1) {
      throw new IllegalArgumentException(
        String.format(
          "Observe is not repeatable, %s",
          o
            .values()
            .stream()
            .map(v -> v.asString())
            .collect(Collectors.toList())
        )
      );
    } else if (o.values().size() == 0) {
      this.register = false;
    } else {
      this.register = o.values().get(0).asBytes()[0] == 0;
    }
  }

  @Override
  public long number() {
    return Observe.number;
  }

  public boolean isRegister() {
    return this.register;
  }

  @Override
  public String toString() {
    return this.register ? "Observe.REGISTER" : "Observe.DEREGISTER";
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Host h -> this.toString().equals(h.toString());
      default -> false;
    };
  }

  @Override
  public List<OptionValue> values() {
    var list = new ArrayList<OptionValue>();
    list.add(
      new dev.toad.msg.owned.OptionValue(
        new byte[] { this.register ? (byte) 0 : (byte) 1 }
      )
    );
    return list;
  }

  @Override
  public String toDebugString() {
    return this.register ? "Observe: Register" : "Observe: Deregister";
  }
}
