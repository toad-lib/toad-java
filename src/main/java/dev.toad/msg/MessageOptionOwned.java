package dev.toad.msg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageOptionOwned implements MessageOption {
  public final long number;
  public final List<MessageOptionValue> values;

  public MessageOptionOwned(MessageOptionRef ref) {
    this.number = ref.number();
    this.values = Arrays.asList(ref.valueRefs())
                        .stream()
                        .map(MessageOptionValueRef::clone)
                        .collect(Collectors.toList());
  }

  public long number() {return this.number;}
  public List<MessageOptionValue> values() {return this.values;}
}
