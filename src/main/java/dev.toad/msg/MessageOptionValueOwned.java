package dev.toad.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageOptionValueOwned implements MessageOptionValue {

  public final byte[] bytes;

  public MessageOptionValueOwned(MessageOptionValueRef ref) {
    this.bytes = ref.asBytes().clone();
  }

  public byte[] asBytes() {
    return this.bytes;
  }

  public String asString() {
    return new String(this.asBytes());
  }
}
