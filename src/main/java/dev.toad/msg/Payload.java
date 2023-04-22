package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import dev.toad.msg.option.ContentFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Payload implements Debug {

  final byte[] bytes;
  final Optional<ContentFormat> contentFormat;

  public static final Eq<Payload> eq = Eq.all(
    List.of(
      Eq.optional(ContentFormat.eq).contramap(Payload::contentFormat),
      Eq.byteArray.contramap(Payload::bytes)
    )
  );

  public static boolean equals(Payload a, Payload b) {
    return (
      Arrays.equals(a.bytes, b.bytes) && a.contentFormat.equals(b.contentFormat)
    );
  }

  public Payload() {
    this.contentFormat = Optional.empty();
    this.bytes = new byte[] {};
  }

  public Payload(byte[] bytes) {
    this.contentFormat = Optional.empty();
    this.bytes = bytes;
  }

  public Payload(ContentFormat contentFormat, byte[] bytes) {
    this.contentFormat = Optional.of(contentFormat);
    this.bytes = bytes;
  }

  public boolean isEmpty() {
    return this.bytes.length == 0;
  }

  @Override
  public String toString() {
    return new String(this.bytes, StandardCharsets.UTF_8);
  }

  public Optional<ContentFormat> contentFormat() {
    return this.contentFormat;
  }

  public byte[] bytes() {
    return this.bytes;
  }

  public static Payload utf8Encoded(ContentFormat contentFormat, String text) {
    return new Payload(contentFormat, text.getBytes(StandardCharsets.UTF_8));
  }

  public static Payload text(String text) {
    return Payload.utf8Encoded(ContentFormat.TEXT, text);
  }

  public static Payload json(String json) {
    return Payload.utf8Encoded(ContentFormat.JSON, json);
  }

  public static Payload linkFormat(String linkFormat) {
    return Payload.utf8Encoded(ContentFormat.LINK_FORMAT, linkFormat);
  }

  public static Payload exi(byte[] exi) {
    return new Payload(ContentFormat.EXI, exi);
  }

  public static Payload octetStream(byte[] bytes) {
    return new Payload(ContentFormat.OCTET_STREAM, bytes);
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Payload p -> Payload.eq.test(this, p);
      default -> false;
    };
  }

  @Override
  public String toDebugString() {
    if (this.contentFormat.map(ContentFormat::isUtf8Text).orElse(false)) {
      return this.toString();
    } else {
      var intList = new ArrayList<Integer>();
      var bytes = this.bytes();
      for (byte b : bytes) {
        intList.add((int) b);
      }

      return String.format("%s", intList.toString());
    }
  }
}
