package dev.toad;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Eq<T> {

  public static final Eq<Short> short_ = new Eq<>((a, b) -> a == b);
  public static final Eq<Integer> int_ = new Eq<>((a, b) -> a == b);
  public static final Eq<Long> long_ = new Eq<>((a, b) -> a == b);
  public static final Eq<String> string = new Eq<>((a, b) ->
    (a != null && b != null && a.equals(b)) || (a == null && b == null)
  );
  public static final Eq<byte[]> byteArray = new Eq<>((a, b) ->
    Arrays.equals(a, b)
  );

  public static final Eq<java.net.InetSocketAddress> socketAddress = new Eq<>(
      (a, b) ->
    a.equals(b)
  );

  public static <T> Eq<T> all(List<Eq<T>> eqs) {
    return new Eq<>((a, b) -> eqs.stream().allMatch(eq -> eq.test(a, b)));
  }

  public static <T> Eq<Optional<T>> optional(Eq<T> eq) {
    return new Eq<>((a, b) -> {
      if (!a.isEmpty() && !b.isEmpty()) {
        return eq.test(a.get(), b.get());
      } else {
        return a.isEmpty() && b.isEmpty();
      }
    });
  }

  public static <K, V> Eq<Map<K, V>> map(Eq<V> eq) {
    return new Eq<>((a, b) -> {
      if (a.entrySet().size() != b.entrySet().size()) {
        return false;
      }

      for (var ent : a.entrySet()) {
        var val = b.get(ent.getKey());
        if (val == null || !eq.test(val, ent.getValue())) {
          return false;
        }
      }

      return true;
    });
  }

  public static <T> Eq<List<T>> list(Eq<T> eq) {
    return new Eq<>((a, b) -> {
      if (a.size() != b.size()) {
        return false;
      }

      for (int i = 0; i < a.size(); i++) {
        if (!eq.test(a.get(i), b.get(i))) {
          return false;
        }
      }

      return true;
    });
  }

  final BiFunction<T, T, Boolean> eq;

  public Eq(BiFunction<T, T, Boolean> eq) {
    this.eq = eq;
  }

  public boolean test(T a, T b) {
    return this.eq.apply(a, b);
  }

  public <U> Eq<U> contramap(Function<U, T> from) {
    return new Eq<>((a, b) -> this.test(from.apply(a), from.apply(b)));
  }
}
