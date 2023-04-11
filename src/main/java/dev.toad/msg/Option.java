package dev.toad.msg;

import java.util.List;

public interface Option {
  public long number();

  public List<OptionValue> values();
}
