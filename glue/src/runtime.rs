use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use toad::platform::Platform;
use toad_jni::java::{self, Object};

use crate::mem::{Shared, SharedMemoryRegion};
use crate::message_ref::MessageRef;
use crate::runtime_config::RuntimeConfig;
use crate::Runtime as ToadRuntime;

pub struct Runtime(java::lang::Object);

impl Runtime {
  pub fn new(e: &mut java::Env, cfg: RuntimeConfig) -> Self {
    static CTOR: java::Constructor<Runtime, fn(RuntimeConfig)> = java::Constructor::new();
    CTOR.invoke(e, cfg)
  }

  pub fn poll_req(&self, e: &mut java::Env) -> Option<MessageRef> {
    static POLL_REQ: java::Method<Runtime, fn() -> java::util::Optional<MessageRef>> =
      java::Method::new("pollReq");
    POLL_REQ.invoke(e, self).to_option(e)
  }

  pub fn config(&self, e: &mut java::Env) -> RuntimeConfig {
    static CONFIG: java::Method<Runtime, fn() -> RuntimeConfig> = java::Method::new("config");
    CONFIG.invoke(e, self)
  }

  fn init_impl(e: &mut java::Env, cfg: RuntimeConfig) -> i64 {
    let r = || ToadRuntime::try_new(cfg.addr(e), cfg.to_toad(e)).unwrap();
    unsafe { crate::mem::Shared::init(r).addr() as i64 }
  }

  fn poll_req_impl(e: &mut java::Env, addr: i64) -> java::util::Optional<MessageRef> {
    match unsafe {
            Shared::deref::<crate::Runtime>(/* TODO */ 0, addr).as_ref()
                                                               .unwrap()
          }.poll_req()
    {
      | Ok(req) => {
        let mr = MessageRef::new(e, req.unwrap().into());
        java::util::Optional::<MessageRef>::of(e, mr)
      },
      | Err(nb::Error::WouldBlock) => java::util::Optional::<MessageRef>::empty(e),
      | Err(nb::Error::Other(err)) => {
        e.throw(format!("{:?}", err)).unwrap();
        java::util::Optional::<MessageRef>::empty(e)
      },
    }
  }
}

java::object_newtype!(Runtime);

impl java::Class for Runtime {
  const PATH: &'static str = package!(dev.toad.ToadRuntime);
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_ToadRuntime_init<'local>(mut e: java::Env<'local>,
                                                              _: JClass<'local>,
                                                              cfg: JObject<'local>)
                                                              -> i64 {
  let e = &mut e;
  let cfg = java::lang::Object::from_local(e, cfg).upcast_to::<RuntimeConfig>(e);

  Runtime::init_impl(e, cfg)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_ToadRuntime_pollReq<'local>(mut e: java::Env<'local>,
                                                                 _: JClass<'local>,
                                                                 addr: i64)
                                                                 -> jobject {
  let e = &mut e;
  Runtime::poll_req_impl(e, addr).yield_to_java(e)
}
