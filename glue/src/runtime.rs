use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use toad::platform::Platform;
use toad_jni::java::{self, Object};

use crate::mem::RuntimeAllocator;
use crate::message_ref::MessageRef;
use crate::runtime_config::RuntimeConfig;
use crate::Runtime as ToadRuntime;

pub struct Runtime(java::lang::Object);

impl Runtime {
  pub fn init(e: &mut java::Env, cfg: RuntimeConfig) -> i64 {
    let r =
      ToadRuntime::try_new(format!("0.0.0.0:{}", cfg.net(e).port(e)), cfg.to_toad(e)).unwrap();
    unsafe { crate::mem::Runtime::alloc(r).addr() as i64 }
  }

  pub fn addr(&self, e: &mut java::Env) -> i64 {
    static ADDR: java::Field<Runtime, i64> = java::Field::new("addr");
    ADDR.get(e, self)
  }

  pub fn ref_(&self, e: &mut java::Env) -> &'static ToadRuntime {
    unsafe { crate::mem::Runtime::deref(self.addr(e)).as_ref().unwrap() }
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

  Runtime::init(e, cfg)
}

// JNIEXPORT jobject JNICALL Java_dev_toad_Runtime_pollReq
//   (JNIEnv *, jobject, jobject);
#[no_mangle]
pub extern "system" fn Java_dev_toad_Runtime_pollReq<'local>(mut e: java::Env<'local>,
                                                             runtime: JObject<'local>,
                                                             cfg: JObject<'local>)
                                                             -> jobject {
  let e = &mut e;
  let runtime = java::lang::Object::from_local(e, runtime).upcast_to::<Runtime>(e);
  match runtime.ref_(e).poll_req() {
    | Ok(req) => {
      let mr = MessageRef::new(e, req.data().msg());
      java::util::Optional::<MessageRef>::of(e, mr).downcast(e)
                                                   .as_raw()
    },
    | Err(nb::Error::WouldBlock) => java::util::Optional::<MessageRef>::empty(e).downcast(e)
                                                                                .as_raw(),
    | Err(nb::Error::Other(err)) => {
      e.throw(format!("{:?}", err)).unwrap();
      core::ptr::null_mut()
    },
  }
}
