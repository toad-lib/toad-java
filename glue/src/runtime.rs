use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use toad::platform::Platform;
use toad_jni::java::{self, Object};

use crate::mem::SharedMemoryRegion;
use crate::message_ref::MessageRef;
use crate::runtime_config::RuntimeConfig;
use crate::Runtime as ToadRuntime;

pub struct Runtime(java::lang::Object);

impl Runtime {
  pub fn get_or_init(e: &mut java::Env, cfg: RuntimeConfig) -> Self {
    static GET_OR_INIT: java::StaticMethod<Runtime, fn(RuntimeConfig) -> Runtime> =
      java::StaticMethod::new("getOrInit");
    GET_OR_INIT.invoke(e, cfg)
  }

  pub fn poll_req(&self, e: &mut java::Env) -> Option<MessageRef> {
    static POLL_REQ: java::Method<Runtime, fn() -> java::util::Optional<MessageRef>> =
      java::Method::new("pollReq");
    POLL_REQ.invoke(e, self).to_option(e)
  }

  pub fn addr(&self, e: &mut java::Env) -> i64 {
    static ADDR: java::Field<Runtime, i64> = java::Field::new("addr");
    ADDR.get(e, self)
  }

  pub fn ref_(&self, e: &mut java::Env) -> &'static ToadRuntime {
    unsafe {
      crate::mem::Shared::deref::<ToadRuntime>(0, self.addr(e)).as_ref()
                                                               .unwrap()
    }
  }

  fn init_impl(e: &mut java::Env, cfg: RuntimeConfig) -> i64 {
    let r =
      || ToadRuntime::try_new(format!("0.0.0.0:{}", cfg.net(e).port(e)), cfg.to_toad(e)).unwrap();
    unsafe { crate::mem::Shared::init(r).addr() as i64 }
  }

  fn poll_req_impl(&self, e: &mut java::Env) -> java::util::Optional<MessageRef> {
    match self.ref_(e).poll_req() {
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
  const PATH: &'static str = package!(dev.toad.Runtime);
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Runtime_init<'local>(mut e: java::Env<'local>,
                                                          _: JClass<'local>,
                                                          cfg: JObject<'local>)
                                                          -> i64 {
  let e = &mut e;
  let cfg = java::lang::Object::from_local(e, cfg).upcast_to::<RuntimeConfig>(e);

  Runtime::init_impl(e, cfg)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Runtime_pollReq<'local>(mut e: java::Env<'local>,
                                                             runtime: JObject<'local>)
                                                             -> jobject {
  let e = &mut e;
  java::lang::Object::from_local(e, runtime).upcast_to::<Runtime>(e)
                                            .poll_req_impl(e)
                                            .yield_to_java(e)
}
