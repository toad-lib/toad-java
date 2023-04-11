package dev.toad.ffi;

import java.lang.ref.Cleaner;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * A native pointer into the memory region shared between toadlib and the jvm
 *
 * Most notably, this ties into all `_Ref` classes' `close()` implementations;
 * when the toad runtime closes these objects, their pointers are `release()`d.
 *
 * Attempts to store and then use one of these Ref objects will throw a
 * `Ptr.ExpiredError`, as control has since been yielded to Rust code and the
 * address we had may have been invalidated by it.
 */
public class Ptr {

  private static volatile HashSet<Long> validAddresses = new HashSet<>();

  protected final long addr;
  private final String clazz;
  private final String trace;

  /**
   * Associate a class instance with a native pointer
   */
  public static synchronized Ptr register(Class c, long addr) {
    var trace = Thread.currentThread().getStackTrace();
    var traceStr = Arrays
      .asList(trace)
      .stream()
      .skip(2)
      .map(StackTraceElement::toString)
      .reduce("", (s, tr) -> s == "" ? tr : s + "\n\t" + tr);

    Ptr.validAddresses.add(addr);
    return new Ptr(addr, c.toString(), traceStr);
  }

  private Ptr(long addr, String clazz, String trace) {
    this.clazz = clazz;
    this.addr = addr;
    this.trace = trace;
  }

  /**
   * Invokes the cleaning action on the object associated with an address
   */
  public synchronized void release() {
    Ptr.validAddresses.remove(this.addr);
  }

  /**
   * Throw `ExpiredError` if object has been leaked
   * outside of its appropriate context.
   */
  public synchronized void ensureValid() {
    if (!Ptr.validAddresses.contains(this.addr)) {
      throw new ExpiredError(this);
    }
  }

  public synchronized long addr() {
    this.ensureValid();
    return this.addr;
  }

  public static class ExpiredError extends Error {

    private static final String fmt =
      "Instance of %s may not be stored by user code.\n" +
      "Object was registered by:\n" +
      ">>>>>>\n" +
      "%s\n" +
      "<<<<<<\n";

    ExpiredError(Ptr ptr) {
      super(String.format(fmt, ptr.clazz, ptr.trace));
    }
  }
}
