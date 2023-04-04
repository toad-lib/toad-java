package dev.toad.msg;

import java.util.List;

public interface MessageOption {
  public long number();
  public List<MessageOptionValue> values();
}
