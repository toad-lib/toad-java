package dev.toad.msg;

public final class Id {

  final int id;

  public Id(int id) {
    this.id = id;
  }

  public int toInt() {
    return this.id;
  }
}
