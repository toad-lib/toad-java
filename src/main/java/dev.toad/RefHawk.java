package dev.toad;

import java.lang.ref.Cleaner;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * Static class used to track pointers issued by rust code
 *
 * When an object instance containing a pointer tracked by RefHawk
 * is not automatically freed before `RefHawk.ensureReleased` invoked,
 * an error is thrown indicating incorrect usage of an object containing
 * a native pointer.
 */
public class RefHawk {

  private static final HashSet<Long> addrs = new HashSet<>();

  private RefHawk() {}

  public static class IllegalStorageOfRefError extends Error {

    private static final String fmt =
      "Instance of %s may not be stored by user code.\n" +
      "Object was registered by:\n" +
      ">>>>>>\n" +
      "%s\n" +
      "<<<<<<\n";

    IllegalStorageOfRefError(Ptr ptr) {
      super(String.format(fmt, ptr.clazz, ptr.trace));
    }
  }

  public static class Ptr {

    protected final long addr;
    private final String clazz;
    private final String trace;

    Ptr(long addr, String clazz, String trace) {
      this.clazz = clazz;
      this.addr = addr;
      this.trace = trace;
    }

    public long addr() {
      RefHawk.ensureValid(this);
      return this.addr;
    }
  }

  /**
   * Associate an object with a raw `long` pointer and a short text
   * describing the scope in which the object is intended to be valid for
   * (e.g. "lambda")
   */
  public static Ptr register(Class c, long addr) {
    var trace = Thread.currentThread().getStackTrace();
    var traceStr = Arrays
      .asList(trace)
      .stream()
      .skip(2)
      .map(StackTraceElement::toString)
      .reduce("", (s, tr) -> s == "" ? tr : s + "\n\t" + tr);

    RefHawk.addrs.add(addr);
    return new Ptr(addr, c.toString(), traceStr);
  }

  /**
   * Invokes the cleaning action on the object associated with an address
   */
  public static void release(Ptr ptr) {
    RefHawk.addrs.remove(ptr.addr);
  }

  /**
   * Throw `IllegalStorageOfRefError` if object has been leaked
   * outside of its appropriate context.
   */
  public static void ensureValid(Ptr ptr) {
    if (!RefHawk.addrs.contains(ptr.addr)) {
      throw new IllegalStorageOfRefError(ptr);
    }
  }
}
