package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.ffi.u16;

public final class Id implements Debug {

  public static native Id defaultId();

  final u16 id;

  public Id(int id) {
    this.id = new u16(id);
  }

  public int toInt() {
    return this.id.intValue();
  }

  @Override
  public String toDebugString() {
    return String.format("Id(%d)", this.toInt());
  }
}
