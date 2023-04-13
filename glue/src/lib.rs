#![feature(strict_provenance)]

use std::ffi::c_void;

use jni::JavaVM;
use mem::SharedMemoryRegion;

mod runtime {
  use std::collections::BTreeMap;

  use toad::platform::Effect;
  use toad::std::{dtls, Platform};
  use toad::step::runtime::std::Runtime as DefaultSteps;
  use toad_jni::java::nio::channels::PeekableDatagramChannel;
  use toad_msg::{OptValue, OptNumber};

  #[derive(Clone, Copy, Debug)]
  pub struct PlatformTypes;

  impl toad::platform::PlatformTypes for PlatformTypes {
    type MessagePayload = Vec<u8>;
    type MessageOptionBytes = Vec<u8>;
    type MessageOptionMapOptionValues = Vec<OptValue<Vec<u8>>>;
    type MessageOptions = BTreeMap<OptNumber, Vec<OptValue<Vec<u8>>>>;
    type Clock = toad::std::Clock;
    type Socket = PeekableDatagramChannel;
    type Effects = Vec<Effect<Self>>;
  }

  pub type Runtime = Platform<dtls::N, DefaultSteps<dtls::N>>;
}

pub use runtime::Runtime;

#[macro_export]
macro_rules! package {
  // allows global compile-time refactoring if the package name ever changes
  (dev.toad.$($thing:ident).+) => {$crate::package!(ext dev.toad.$($thing).+)};
  (ext $start:ident.$($thing:ident).+) => {concat!(stringify!($start), $("/", stringify!($thing)),+)};
}

pub mod dev;
pub mod mem;

#[no_mangle]
pub extern "system" fn JNI_OnLoad(jvm: JavaVM, _: *const c_void) -> i32 {
  toad_jni::global::init_with(jvm);
  jni::sys::JNI_VERSION_1_8
}

#[no_mangle]
pub extern "system" fn JNI_OnUnload(_: JavaVM, _: *const c_void) {
  unsafe { mem::Shared::dealloc() }
}

#[cfg(all(test, feature = "e2e"))]
pub mod e2e;

#[cfg(test)]
pub mod test {
  use std::net::{Ipv4Addr, SocketAddr};
  use std::sync::Once;

  use jni::{InitArgsBuilder, JavaVM};
  use toad::config::Config;
  use toad::retry::Strategy;
  use toad::time::Millis;
  use toad_jni::java;

  use crate::dev;

  pub fn init<'a>() -> java::Env<'a> {
    static INIT: Once = Once::new();
    INIT.call_once(|| {
      let jvm =
        JavaVM::new(InitArgsBuilder::new().option("-Djava.library.path=/home/orion/src/toad-lib/toad-java/target/glue/debug/")
                                          .option("-Djava.class.path=../target/scala-3.2.2/classes")
                                          .option("--enable-preview")
                                          .build()
                                          .unwrap()).unwrap();
      toad_jni::global::init_with(jvm);
    });

    toad_jni::global::jvm().attach_current_thread_permanently()
                           .unwrap()
  }

  #[test]
  fn package() {
    assert_eq!(package!(dev.toad.msg.Foo.Bar.Baz),
               "dev/toad/msg/Foo/Bar/Baz");
    assert_eq!(package!(ext java.lang.String), "java/lang/String");
  }

  #[test]
  fn runtime_config() {
    let mut e = init();
    let e = &mut e;

    let r = dev::toad::Config::new(e,
                                   Config::default(),
                                   SocketAddr::new(Ipv4Addr::UNSPECIFIED.into(), 5683));
    assert_eq!(r.to_toad(e), Config::default());
  }

  #[test]
  fn retry_strategy() {
    let mut e = init();
    let e = &mut e;

    let r = Strategy::Exponential { init_min: Millis::new(0),
                                    init_max: Millis::new(100) };
    assert_eq!(dev::toad::RetryStrategy::from_toad(e, r).to_toad(e), r);

    let r = Strategy::Delay { min: Millis::new(0),
                              max: Millis::new(100) };
    assert_eq!(dev::toad::RetryStrategy::from_toad(e, r).to_toad(e), r);
  }

  #[test]
  fn uint() {
    let mut e = init();
    let e = &mut e;

    macro_rules! case {
      ($u:ident) => {{
        assert_eq!(dev::toad::ffi::$u::from_rust(e, $u::MAX).to_rust(e),
                   $u::MAX);
        assert_eq!(dev::toad::ffi::$u::from_rust(e, 0).to_rust(e), 0);
      }};
    }

    case!(u64);
    case!(u32);
    case!(u16);
    case!(u8);
  }
}
