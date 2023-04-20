package dev.toad.msg.option;

import dev.toad.ffi.u16;
import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.ArrayList;
import java.util.List;

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
    super(new ContentFormat(o).value());
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
}
