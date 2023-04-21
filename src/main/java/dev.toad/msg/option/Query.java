package dev.toad.msg.option;

import dev.toad.Eq;
import dev.toad.msg.Option;
import dev.toad.msg.OptionValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

public final class Query implements Option {

  final ArrayList<String> query;
  public static final long number = 15;
  public static final Eq<Query> eq = Eq
    .<String, List<Value>>map(Eq.list(Value.eq))
    .contramap(Query::toMap);

  public Query(Option o) {
    if (o.number() != Query.number) {
      throw new IllegalArgumentException(
        String.format("%d != Query number %d", o.number(), Query.number)
      );
    }

    this.query =
      o
        .values()
        .stream()
        .map(v -> v.asString())
        .collect(Collectors.toCollection(() -> new ArrayList<>()));
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

  @Override
  public String toDebugString() {
    return String.format("Uri-Query: %s", this.toString());
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Query q -> Query.eq.test(this, q);
      default -> false;
    };
  }

  public static final class Value {

    public static final Eq<Value> eq = Eq
      .optional(Eq.string)
      .contramap(Value::value);

    final Optional<String> val;

    public Value(String val) {
      if (val == null || val.isEmpty()) {
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
        return "Query.Value(\"" + this.value().get() + "\")";
      } else {
        return "Query.Value.empty()";
      }
    }

    @Override
    public boolean equals(Object other) {
      return switch (other) {
        case Value v -> Value.eq.test(this, v);
        default -> false;
      };
    }
  }
}
