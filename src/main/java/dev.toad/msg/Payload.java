package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.msg.option.ContentFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;

public final class Payload implements Debug {

  final byte[] bytes;
  final Optional<ContentFormat> contentFormat;

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
