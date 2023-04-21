package dev.toad.msg.option;

import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Path implements Option {

  final ArrayList<String> segments;

  public static final long number = 11;

  public Path(Option o) {
    if (o.number() != Path.number) {
      throw new IllegalArgumentException(
        String.format("%d != Path number %d", o.number(), Path.number)
      );
    }

    this.segments =
      o
        .values()
        .stream()
        .map(v -> v.asString())
        .collect(Collectors.toCollection(() -> new ArrayList<>()));
  }

  public Path(String path) {
    if (path == null || path.isEmpty()) {
      this.segments = new ArrayList<>();
    } else {
      if (path.startsWith("/")) {
        path = path.substring(1);
      }

      this.segments = new ArrayList<>(Arrays.asList(path.trim().split("/")));
    }
  }

  public boolean equals(Path other) {
    return this.segments == other.segments;
  }

  public List<String> segments() {
    return this.segments;
  }

  public boolean matches(String str) {
    return this.toString().trim().equals(str.trim());
  }

  @Override
  public long number() {
    return Path.number;
  }

  @Override
  public String toString() {
    return String.join("/", this.segments);
  }

  @Override
  public String toDebugString() {
    return String.format("Uri-Path: %s", this.toString());
  }

  @Override
  public List<OptionValue> values() {
    return this.segments.stream()
      .map(s -> new dev.toad.msg.owned.OptionValue(s))
      .collect(Collectors.toList());
  }
}
