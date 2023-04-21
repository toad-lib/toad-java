package dev.toad.msg;

import dev.toad.Debug;
import dev.toad.Eq;
import dev.toad.ffi.u16;

public final class Id implements Debug {

  public static native Id defaultId();

  public static final Eq<Id> eq = Eq.int_.contramap(Id::toInt);

  final u16 id;

  public Id(int id) {
    this.id = new u16(id);
  }

  public int toInt() {
    return this.id.intValue();
  }

  @Override
  public boolean equals(Object other) {
    return switch (other) {
      case Id i -> Id.eq.test(this, i);
      default -> false;
    };
  }

  @Override
  public String toDebugString() {
    return String.format("Id(%d)", this.toInt());
  }
}
