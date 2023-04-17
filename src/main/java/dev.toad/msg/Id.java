package dev.toad.msg;

import dev.toad.ffi.u16;

public final class Id {

  public static native Id defaultId();

  final u16 id;

  public Id(int id) {
    this.id = new u16(id);
  }

  public int toInt() {
    return this.id.intValue();
  }
}
