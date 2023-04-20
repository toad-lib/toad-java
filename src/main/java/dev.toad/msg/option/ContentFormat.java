package dev.toad.msg.option;

import dev.toad.ffi.u16;
import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public sealed class ContentFormat implements Option permits Accept {

  final u16 value;

  public static final long number = 12;

  public ContentFormat(Option o) {
    if (o.number() != ContentFormat.number) {
      throw new IllegalArgumentException(
        String.format("%d != ContentFormat number %d", o.number(), Path.number)
      );
    }

    if (o.values().size() > 1) {
      throw new IllegalArgumentException(
        String.format(
          "ContentFormat is not repeatable, %s",
          o
            .values()
            .stream()
            .map(v -> v.asString())
            .collect(Collectors.toList())
        )
      );
    }

    var bytes = o.values().get(0).asBytes();

    var buf = ByteBuffer.wrap(bytes);
    if (bytes.length == 1) {
      this.value = new u16(buf.get());
    } else if (bytes.length == 2) {
      this.value = new u16(buf.getShort());
    } else if (bytes.length == 3) {
      buf.put(0, (byte) 0);
      this.value = new u16(buf.getInt());
    } else {
      this.value = new u16(buf.getInt());
    }
  }

  ContentFormat(int value) {
    this.value = new u16(value);
  }

  public static final ContentFormat TEXT = ContentFormat.custom(0);
  public static final ContentFormat LINK_FORMAT = ContentFormat.custom(40);
  public static final ContentFormat XML = ContentFormat.custom(41);
  public static final ContentFormat OCTET_STREAM = ContentFormat.custom(42);
  public static final ContentFormat EXI = ContentFormat.custom(47);
  public static final ContentFormat JSON = ContentFormat.custom(50);
  public static final ContentFormat CBOR = ContentFormat.custom(60);

  public static final ContentFormat IMAGE_GIF = ContentFormat.custom(21);
  public static final ContentFormat IMAGE_JPG = ContentFormat.custom(22);
  public static final ContentFormat IMAGE_PNG = ContentFormat.custom(23);
  public static final ContentFormat IMAGE_SVG = ContentFormat.custom(30000);

  public static final ContentFormat JAVASCRIPT = ContentFormat.custom(10002);
  public static final ContentFormat CSS = ContentFormat.custom(20000);

  public static ContentFormat custom(int value) {
    return new ContentFormat(value);
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case ContentFormat cf -> this.equals(cf);
      default -> false;
    };
  }

  public boolean equals(ContentFormat other) {
    return this.value() == other.value();
  }

  public String toString() {
    return String.format("ContentFormat(%d)", this.value());
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
