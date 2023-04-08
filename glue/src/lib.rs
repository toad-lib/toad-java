#![feature(strict_provenance)]

pub type Runtime =
  toad::std::Platform<toad::std::dtls::N, toad::step::runtime::std::Runtime<toad::std::dtls::N>>;

#[macro_export]
macro_rules! package {
  // allows global compile-time refactoring if the package name ever changes
  (dev.toad.$($thing:ident).+) => {$crate::package!(ext dev.toad.$($thing).+)};
  (ext $start:ident.$($thing:ident).+) => {concat!(stringify!($start), $("/", stringify!($thing)),+)};
}

pub mod mem;
pub mod message_code;
pub mod message_opt_ref;
pub mod message_opt_value_ref;
pub mod message_ref;
pub mod message_type;
pub mod retry_strategy;
pub mod runtime;
pub mod runtime_config;
pub mod uint;

#[cfg(test)]
mod tests {
  use std::sync::Once;

  use jni::{InitArgsBuilder, JavaVM};
  use toad::retry::Strategy;
  use toad::time::Millis;
  use toad_jni::java;

  use crate::retry_strategy::RetryStrategy;
  use crate::runtime_config::RuntimeConfig;

  static INIT: Once = Once::new();
  pub fn init<'a>() -> java::Env<'a> {
    INIT.call_once(|| {
      let jvm =
        JavaVM::new(InitArgsBuilder::new().option("--enable-preview")
                                          .option("-Djava.class.path=../target/scala-3.2.2/classes/")
                                          .build()
                                          .unwrap()).unwrap();
      toad_jni::global::init_with(jvm);
    });

    toad_jni::global::jvm().attach_current_thread_permanently()
                           .unwrap();
    toad_jni::global::jvm().get_env().unwrap()
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

    let r = RuntimeConfig::new(e);
    assert_eq!(r.to_toad(e), Default::default());
  }

  #[test]
  fn retry_strategy() {
    let mut e = init();
    let e = &mut e;

    let r = Strategy::Exponential { init_min: Millis::new(0),
                                    init_max: Millis::new(100) };
    assert_eq!(RetryStrategy::from_toad(e, r).to_toad(e), r);

    let r = Strategy::Delay { min: Millis::new(0),
                              max: Millis::new(100) };
    assert_eq!(RetryStrategy::from_toad(e, r).to_toad(e), r);
  }

  #[test]
  fn uint() {
    use crate::uint;

    let mut e = init();
    let e = &mut e;

    macro_rules! case {
      ($u:ident) => {{
        assert_eq!(uint::$u::from_rust(e, $u::MAX).to_rust(e), $u::MAX);
        assert_eq!(uint::$u::from_rust(e, 0).to_rust(e), 0);
      }};
    }

    case!(u64);
    case!(u32);
    case!(u16);
    case!(u8);
  }
}
