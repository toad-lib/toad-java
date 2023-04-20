package dev.toad.msg;

import java.util.Optional;
import dev.toad.msg.option.ContentFormat;
import java.nio.charset.StandardCharsets;

public final class Payload {
  final byte[] bytes;
  final Optional<ContentFormat> contentFormat;

  public Payload(byte[] bytes) {
    this.contentFormat = Optional.empty();
    this.bytes = bytes;
  }

  public Payload(ContentFormat contentFormat, byte[] bytes) {
    this.contentFormat = Optional.of(contentFormat);
    this.bytes = bytes;
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
}
