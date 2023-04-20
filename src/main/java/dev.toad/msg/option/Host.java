package dev.toad.msg.option;

import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Host implements Option {

  final String host;

  public static final long number = 3;

  public Host(Option o) {
    if (o.number() != Host.number) {
      throw new IllegalArgumentException(
        String.format("%d != Host number %d", o.number(), Path.number)
      );
    }

    if (o.values().size() > 1) {
      throw new IllegalArgumentException(
        String.format(
          "Host is not repeatable, %s",
          o
            .values()
            .stream()
            .map(v -> v.asString())
            .collect(Collectors.toList())
        )
      );
    }

    this.host = o.values().get(0).asString();
  }

  public Host(String host) {
    this.host = host;
  }

  @Override
  public long number() {
    return Host.number;
  }

  @Override
  public String toString() {
    return this.host;
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
    list.add(new dev.toad.msg.owned.OptionValue(this.host));
    return list;
  }
}
