package dev.toad;

import dev.toad.msg.MessageRef;
import java.util.Optional;

public class Runtime {

  private final long addr;

  private static native long init(RuntimeOptions o);
  private native Optional<MessageRef> pollReq(RuntimeOptions o);

  public Runtime(RuntimeOptions o) {
    this.addr = Runtime.init(o);
  }
}
