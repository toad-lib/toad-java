package dev.toad.msg.option;

import dev.toad.ffi.u16;
import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Accept extends ContentFormat implements Option {

  public static final long number = 17;

  public static final Accept TEXT = new Accept(ContentFormat.TEXT);
  public static final Accept LINK_FORMAT = new Accept(
    ContentFormat.LINK_FORMAT
  );
  public static final Accept XML = new Accept(ContentFormat.XML);
  public static final Accept OCTET_STREAM = new Accept(
    ContentFormat.OCTET_STREAM
  );
  public static final Accept EXI = new Accept(ContentFormat.EXI);
  public static final Accept JSON = new Accept(ContentFormat.JSON);

  Accept(int value) {
    super(value);
  }

  public long number() {
    return Accept.number;
  }

  public Accept(Option o) {
    super(0);
    if (o.number() != Accept.number) {
      throw new IllegalArgumentException(
        String.format("%d != Accept number %d", o.number(), Path.number)
      );
    }

    if (o.values().size() > 1) {
      throw new IllegalArgumentException(
        String.format(
          "Accept is not repeatable, %s",
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

  public Accept(ContentFormat format) {
    this(format.value());
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Accept cf -> this.equals(cf);
      default -> false;
    };
  }

  public boolean equals(Accept other) {
    return this.value.equals(other.value);
  }

  @Override
  public String toDebugString() {
    return String.format("Accept: %s", this.toMimeType());
  }
}
