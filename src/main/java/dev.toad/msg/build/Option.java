package dev.toad.msg.build;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Option implements OptionNeeds.Number {

  Optional<Long> number = Optional.empty();
  ArrayList<byte[]> values = new ArrayList<>();

  Option() {}

  public static OptionNeeds.Number builder() {
    return new Option();
  }

  public Option number(long num) {
    this.number = Optional.of(num);
    return this;
  }

  public Option addValue(byte[] bytes) {
    this.values.add(bytes);
    return this;
  }

  public dev.toad.msg.owned.Option build() {
    return new dev.toad.msg.owned.Option(
      this.number.get(),
      this.values.stream()
        .map(bytes -> new dev.toad.msg.owned.OptionValue(bytes))
        .collect(Collectors.toCollection(() -> new ArrayList<>()))
    );
  }
}
