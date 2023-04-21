import sys.process._

class Glue extends munit.FunSuite {
  test("cargo test") {
    // Seq(
    //   "sh",
    //   "-c",
    //   "cd glue; RUST_BACKTRACE=full cargo test --quiet --features e2e"
    // ).!!
  }
}
