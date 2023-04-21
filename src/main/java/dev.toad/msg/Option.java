package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import dev.toad.msg.option.*;
import java.util.List;
import java.util.stream.Collectors;

public interface Option extends Debug {
  public long number();

  public List<OptionValue> values();

  public static Eq<Option> eq() {
    return Eq.all(
      List.of(
        Eq.long_.contramap(o -> o.number()),
        Eq.list(OptionValue.eq()).contramap(Option::values)
      )
    );
  }

  public default boolean equals(Option o) {
    return Option.eq().test(this, o);
  }

  @Override
  public default String toDebugString() {
    if (this.number() == Path.number) {
      return new Path(this).toDebugString();
    } else if (this.number() == Host.number) {
      return new Host(this).toDebugString();
    } else if (this.number() == Query.number) {
      return new Query(this).toDebugString();
    } else if (this.number() == Accept.number) {
      return new Accept(this).toDebugString();
    } else if (this.number() == ContentFormat.number) {
      return new ContentFormat(this).toDebugString();
    } else {
      return String.format(
        "Option(%d): %s",
        this.number(),
        this.values()
          .stream()
          .map(OptionValue::toDebugString)
          .collect(Collectors.toList())
      );
    }
  }
}
