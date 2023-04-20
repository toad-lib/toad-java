package dev.toad.msg.option;

import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

public final class Query implements Option {

  final ArrayList<String> query;
  public static final long number = 15;

  public Query(Option o) {
    if (o.number() != Query.number) {
      throw new IllegalArgumentException(String.format("%d != Query number %d", o.number(), Query.number));
    }

    this.query = o.values().stream().map(v -> v.asString()).collect(Collectors.toCollection(() -> new ArrayList<>()));
  }

  public Query(String query) {
    if (query == null || query.isEmpty()) {
      this.query = new ArrayList<>();
    } else {
      this.query = new ArrayList<>(Arrays.asList(query.split("&")));
    }
  }

  public Query(List<String> query) {
    this.query = new ArrayList<>(query);
  }

  public Map<String, List<Value>> toMap() {
    Function<String[], String> key = segs -> segs[0];
    Function<String[], List<Value>> value = segs -> {
      var vals = new ArrayList<Value>();
      if (segs.length == 2) {
        vals.add(new Value(segs[1]));
      } else {
        vals.add(new Value(null));
      }

      return vals;
    };

    BinaryOperator<List<Value>> merge = (a, b) -> {
      a.addAll(b);
      return a;
    };

    return this.query.stream()
      .map(v -> v.split("=", 2))
      .collect(Collectors.toMap(key, value, merge, () -> new HashMap<>()));
  }

  @Override
  public long number() {
    return Query.number;
  }

  @Override
  public String toString() {
    return String.join("&", this.query);
  }

  @Override
  public List<OptionValue> values() {
    return this.query.stream()
      .map(q -> new dev.toad.msg.owned.OptionValue(q))
      .collect(Collectors.toList());
  }

  public static final class Value {
    final Optional<String> val;

    public Value(String val) {
      if (val == null || val == "") {
        this.val = Optional.empty();
      } else {
        this.val = Optional.of(val);
      }
    }

    public static Value empty() {
      return new Value(null);
    }

    public Optional<String> value() {
      return this.val;
    }

    @Override
    public String toString() {
      if (!this.value().isEmpty()) {
        return "Query.Value(\""+this.value().get()+"\")";
      } else {
        return "Query.Value.empty()";
      }
    }

    public boolean equals(Value other) {
      return this.toString().equals(other.toString());
    }

    @Override
    public boolean equals(Object other) {
      return switch(other) {
        case Value v -> this.equals(v);
        default -> false;
      };
    }
  }
}
