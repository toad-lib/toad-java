#![feature(strict_provenance)]

use std::ffi::c_void;

use jni::JavaVM;
use mem::SharedMemoryRegion;

mod runtime {
  use std::collections::BTreeMap;

  use toad::config::Config;
  use toad::platform::{Effect, Platform};
  use toad::step::runtime::Runtime as DefaultSteps;
  use toad_jni::java::io::IOException;
  use toad_jni::java::nio::channels::PeekableDatagramChannel;
  use toad_jni::java::util::logging::{ConsoleHandler, Level, Logger};
  use toad_jni::java::{self};
  use toad_msg::{OptNumber, OptValue};

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

  type Steps = DefaultSteps<PlatformTypes, naan::hkt::Vec, naan::hkt::BTreeMap>;

  pub struct Runtime {
    steps: Steps,
    config: Config,
    channel: PeekableDatagramChannel,
    clock: toad::std::Clock,
    logger: Logger,
  }

  impl Runtime {
    pub fn new(e: &mut java::Env,
               log_level: Level,
               config: Config,
               channel: PeekableDatagramChannel)
               -> Self {
      let logger = Logger::get_logger(e, "dev.toad");

      if logger.uses_parent_handlers(e) {
        let handler = ConsoleHandler::new(e);
        handler.set_level(e, log_level);

        logger.use_parent_handlers(e, false);
        logger.add_handler(e, handler.to_handler());
        logger.set_level(e, log_level);
      }

      Self { steps: Default::default(),
             config,
             channel,
             clock: toad::std::Clock::new(),
             logger }
    }
  }

  impl Platform<Steps> for Runtime {
    type Types = PlatformTypes;
    type Error = IOException;

    fn log(&self, level: log::Level, msg: toad::todo::String<1000>) -> Result<(), Self::Error> {
      let mut e = java::env();
      self.logger
          .log(&mut e, Level::from_log_level(level), msg.as_str());
      Ok(())
    }

    fn config(&self) -> toad::config::Config {
      self.config
    }

    fn steps(&self) -> &Steps {
      &self.steps
    }

    fn socket(&self) -> &PeekableDatagramChannel {
      &self.channel
    }

    fn clock(&self) -> &toad::std::Clock {
      &self.clock
    }
  }
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

  use std::path::PathBuf;
  use std::process::Command;
  use std::sync::Once;

  use jni::{InitArgsBuilder, JavaVM};
  use toad::config::Config;
  use toad::retry::Strategy;
  use toad::time::Millis;
  use toad_jni::java::{self, Class, ResultExt};

  use crate::dev;

  pub fn init<'a>() -> java::Env<'a> {
    static INIT: Once = Once::new();
    INIT.call_once(|| {
          let repo_root = Command::new("git").arg("rev-parse")
                                             .arg("--show-toplevel")
                                             .output()
                                             .unwrap();
          assert!(repo_root.status.success());

          let lib_path = String::from_utf8(repo_root.stdout).unwrap()
                                                            .trim()
                                                            .to_string();
          let lib_path = PathBuf::from(lib_path).join("target/glue/debug");

          let jvm =
        JavaVM::new(InitArgsBuilder::new().option(format!("-Djava.library.path={}",
                                                          lib_path.to_string_lossy()))
                                          .option("-Djava.class.path=../target/scala-3.2.2/classes")
                                          .option("--enable-preview")
                                          .build()
                                          .unwrap()).unwrap();
          toad_jni::global::init_with(jvm);
        });

    let mut env = toad_jni::global::jvm().attach_current_thread_permanently()
                                         .unwrap();

    env.call_static_method(crate::dev::toad::Toad::PATH, "loadNativeLib", "()V", &[])
       .unwrap_java(&mut env);

    env
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

    let r = dev::toad::Config::new(e, Config::default());
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
