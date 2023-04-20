package dev.toad.msg;

import java.util.List;

public interface Option {
  public long number();

  public List<OptionValue> values();

  public default boolean equals(Option o) {
    return this.number() == o.number() && this.values().equals(o.values());
  }
}
