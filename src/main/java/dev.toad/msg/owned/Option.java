package dev.toad.msg.owned;

import dev.toad.ffi.u32;
import dev.toad.msg.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Option implements dev.toad.msg.Option {

  final u32 number;
  final ArrayList<dev.toad.msg.owned.OptionValue> values;

  public Option(long number, ArrayList<dev.toad.msg.owned.OptionValue> values) {
    this.number = new u32(number);
    this.values = values;
  }

  public Option(dev.toad.msg.ref.Option ref) {
    this(
      ref.number(),
      Arrays
        .asList(ref.valueRefs())
        .stream()
        .map(dev.toad.msg.ref.OptionValue::toOwned)
        .collect(Collectors.toCollection(() -> new ArrayList<>()))
    );
  }

  public long number() {
    return this.number.longValue();
  }

  public List<dev.toad.msg.OptionValue> values() {
    return List.copyOf(this.values);
  }

  public boolean equals(Object other) {
    return switch (other) {
      case dev.toad.msg.Option o -> o.equals(this);
      default -> false;
    };
  }
}
