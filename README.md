## toad-java

This repo contains the Java & Scala APIs for the [toad](https://github.com/toad-lib/toad) CoAP network runtime.

### Developing
#### Developing - Clone
```sh
> git clone git@github.com:toad-lib/toad-java toad-java
> cd toad-java
```

#### Developing - Tooling
##### Developing - Tooling - System
If on a minimal os (ex. bare debian image) install if not present:
- a C compiler, examples:
   - [debian `apt install gcc`](https://packages.debian.org/stable/devel/gcc)
   - [macos `brew install gcc`](https://formulae.brew.sh/formula/gcc)
   - [macos - xcode commandline tools](https://developer.apple.com/xcode/resources/)
- openssl, examples:
   - [debian `apt install libssl-dev`](https://packages.debian.org/stable/devel/gcc)
   - [macos `brew install openssl`](https://formulae.brew.sh/formula/gcc)
- (debian only) `apt install pkg-config`

##### Developing - Tooling - asdf
this repo is configured to use asdf for managing versions of rust, the JVM, and coursier
used by this project.

To use it follow the [asdf installation guide](https://asdf-vm.com/guide/getting-started.html#official-download)
then run the following in bash or zsh in the directory you cloned the repo to:

```sh
> asdf install
> asdf plugin add java     # skip if you have system install of JDK version 20
> asdf plugin add rust     # skip if you have system install of Rust
> asdf plugin add coursier # skip if you have system install of coursier
```

##### Developing - Tooling - Manual
- install [openjdk](https://jdk.java.net/20/) _or equivalent_ version 20 or higher
  - after installing, ensure that `$JAVA_HOME` correctly refers to JDK 20 (`java -version` should output `openjdk version "20" 202x-xx-xx`)
- install [coursier](https://get-coursier.io/docs/cli-install)
- install [rust](https://rustup.rs)

##### Developing - Tooling - sbt
After following the above steps, add coursier-installed binaries to your PATH, ex with:
```sh
> echo 'export PATH=$PATH:/root/.local/share/coursier/bin' >> ~/.zshrc
```

Then install `sbt`
```sh
> coursier install sbt
```

#### Developing - Rust
There is a non-trivial amount of nasty glue code necessary to support the interop between
`toad` (a rust library) and `toad-java` (a java project).

There is a published library [`toad-jni`](https://github.com/toad-lib/toad/tree/main/toad-jni) which contains
general abstractions that are useful for this project and may be useful to others.
This includes things like _"high-level rust struct for `java.util.ArrayList`
doing nice things like implementing [`Iterator`](https://doc.rust-lang.org/nightly/core/iter/trait.Iterator.html)."_

Separately is a rust project in this repository, `./toad-java-glue-rs/`.
This is *not* a published crate and is instead source code for a
shared library specifically to implement native java methods in this java project.

Development tips specific to the glue lib can be found in `./toad-java-glue-rs/README.md`.

#### Developing - Build
the sbt command `compile` (`sbt compile` or `compile` in the `sbt` shell)
is the only required build step for this project.

`sbt compile` can be broken into the following steps:
 1. run `javac` to generate C headers for java files with `native` function requirements
 1. dump the headers into `./toad-java-glue-rs/target/debug/`
    - _(dual purpose; a native interface available to manually cross-check against the hard rust implementation, as well as providing an interface to the built library artifact)_
 1. run `cargo build` within `./toad-java-glue-rs/`
 1. build java & scala sources to `./target/`

### Tests
todo
