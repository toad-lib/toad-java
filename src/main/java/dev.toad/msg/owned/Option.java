package dev.toad.msg.owned;

import dev.toad.msg.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Option implements dev.toad.msg.Option {

  final long number;
  final List<dev.toad.msg.OptionValue> values;

  public Option(long number, List<dev.toad.msg.OptionValue> values) {
    this.number = number;
    this.values = values;
  }

  public Option(dev.toad.msg.ref.Option ref) {
    this(
      ref.number(),
      Arrays
        .asList(ref.valueRefs())
        .stream()
        .map(dev.toad.msg.ref.OptionValue::clone)
        .collect(Collectors.toList())
    );
  }

  public long number() {
    return this.number;
  }

  public List<dev.toad.msg.OptionValue> values() {
    return this.values;
  }
}
