package dev.toad.msg.option;

import dev.toad.ffi.u16;
import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.ArrayList;
import java.util.List;

public sealed class ContentFormat implements Option permits Accept {

  final u16 value;

  public static final long number = 12;

  ContentFormat(int value) {
    this.value = new u16(value);
  }

  public static final ContentFormat TEXT = ContentFormat.custom(0);
  public static final ContentFormat LINK_FORMAT = ContentFormat.custom(40);
  public static final ContentFormat XML = ContentFormat.custom(41);
  public static final ContentFormat OCTET_STREAM = ContentFormat.custom(42);
  public static final ContentFormat EXI = ContentFormat.custom(47);
  public static final ContentFormat JSON = ContentFormat.custom(50);

  public static ContentFormat custom(int value) {
    return new ContentFormat(value);
  }

  public boolean equals(ContentFormat other) {
    return this.value == other.value;
  }

  public long number() {
    return ContentFormat.number;
  }

  public int value() {
    return this.value.intValue();
  }

  public List<OptionValue> values() {
    var list = new ArrayList<OptionValue>();
    list.add(new dev.toad.msg.owned.OptionValue(this.value.toBytes()));
    return list;
  }
}
