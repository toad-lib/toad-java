#![feature(strict_provenance)]

use jni::objects::{JClass, JObject};
use jni::JNIEnv;
use toad::std::{dtls, Platform as Std};
use toad::step::runtime::std::Runtime;

pub static mut RUNTIME: *const Std<dtls::N, Runtime<dtls::N>> = std::ptr::null();

pub unsafe fn with_runtime_provenance<T>(addr: i64) -> *mut T {
  RUNTIME.with_addr(addr as usize).cast::<T>().cast_mut()
}

#[macro_export]
macro_rules! package {
  // allows global compile-time refactoring if the package name ever changes
  (dev.toad.$($thing:ident).+) => {$crate::package!(ext dev.toad.$($thing).+)};
  (ext $start:ident.$($thing:ident).+) => {concat!(stringify!($start), $("/", stringify!($thing)),+)};
}

pub mod uint;
pub mod message_code;
pub mod message_opt_ref;
pub mod message_opt_value_ref;
pub mod message_ref;
pub mod message_type;
pub mod retry_strategy;
pub mod runtime_config;

// Class:     dev_toad_Runtime
// Method:    init
// Signature: (Ldev/toad/RuntimeOptions;)V
// JNIEXPORT void JNICALL Java_dev_toad_Runtime_init
//   (JNIEnv *, jclass, jobject);
pub unsafe extern "system" fn Java_dev_toad_Runtime_init<'local>(mut env: JNIEnv<'local>,
                                                                 _: JClass<'local>,
                                                                 cfg: JObject<'local>) {
}

#[cfg(test)]
mod tests {
  use jni::{InitArgsBuilder, JavaVM};
  use toad::retry::Strategy;
  use toad::time::Millis;

  use crate::retry_strategy::RetryStrategy;
  use crate::runtime_config::RuntimeConfig;

  #[test]
  fn package() {
    assert_eq!(package!(dev.toad.msg.Foo.Bar.Baz),
               "dev/toad/msg/Foo/Bar/Baz");
    assert_eq!(package!(ext java.lang.String), "java/lang/String");
  }

  #[test]
  fn jvm_tests() {
    let jvm =
      JavaVM::new(InitArgsBuilder::new().option("--enable-preview")
                                        .option("-Djava.class.path=../target/scala-3.2.2/classes/")
                                        .build()
                                        .unwrap()).unwrap();
    jvm.attach_current_thread_permanently();
    toad_jni::global::init_with(jvm);

    let mut e = toad_jni::global::env();
    let e = &mut e;

    let r = RuntimeConfig::new(e);
    assert_eq!(r.to_toad(e), Default::default());

    let r = Strategy::Exponential { init_min: Millis::new(0),
                                    init_max: Millis::new(100) };
    assert_eq!(RetryStrategy::from_toad(e, r).to_toad(e), r);

    let r = Strategy::Delay { min: Millis::new(0),
                              max: Millis::new(100) };
    assert_eq!(RetryStrategy::from_toad(e, r).to_toad(e), r);
  }
}
