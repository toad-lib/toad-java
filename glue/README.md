## toad-java-glue
Implementations of native java methods and state for
running `toad` with the JVM

### nightly
this project builds on the nightly channel so that it can
use the pointer strict provenance API (explained in [# pub static mut RUNTIME](#pub-static-mut-runtime))

### integers
all integers stored in java objects to be passed to rust code
should use the `dev.toad.ffi.uX` compat classes to ensure
that the primitive casts in rust succeed.

### unsafe
#### externs
no extern fns called by java should be decorated `unsafe` and should instead be
safe functions with `unsafe` expression bodies to clearly denote the patterns
of unsafety within.

#### justification
with 1 exception described below in [# pub static mut RUNTIME](#pub-static-mut-runtime),
all uses of `unsafe` in safe functions are accompanied
by a `// SAFETY` comment justifying its use and explaining
the risks (or not) of memory defects and UB.

#### pub static mut RUNTIME
`unsafe` is used in an **unjustified** manner to cast `long` addresses
into pointers to the `RUNTIME` static or data within.

The `RUNTIME` static is created in a way such that the location in
memory does not move. This means that addresses issued by rust code
may be stored safely in java objects to be eventually passed back to
rust code to perform operations on the rust structs.

Addresses are issued by rust code using the [strict provenance API](https://doc.rust-lang.org/nightly/std/ptr/index.html#strict-provenance)
to avoid direct integer <> pointer casts, as well as to theoretically
provide runtime guarantees that the addresses gotten from java do not
attempt to access memory regions outside of the runtime data structure.
