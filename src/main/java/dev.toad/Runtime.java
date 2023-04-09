package dev.toad;

import dev.toad.msg.MessageRef;
import java.util.Optional;

public class Runtime {

  static {
    System.loadLibrary("toad_java_glue");
  }

  private final long addr;

  private static native long init(RuntimeOptions o);
  private native Optional<MessageRef> pollReq();

  public static Runtime getOrInit(RuntimeOptions o) {
    return new Runtime(o);
  }

  public Runtime(RuntimeOptions o) {
    this.addr = Runtime.init(o);
  }
}
