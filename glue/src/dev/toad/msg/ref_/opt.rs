use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use jni::JNIEnv;
use toad_jni::java::lang::Throwable;
use toad_jni::java::{self, Object, ResultYieldToJavaOrThrow};
use toad_msg::OptNumber;

use super::OptValue;
use crate::dev::toad::ffi::Ptr;
use crate::mem::{Shared, SharedMemoryRegion};

pub struct Opt(pub java::lang::Object);

java::object_newtype!(Opt);

impl java::Class for Opt {
  const PATH: &'static str = package!(dev.toad.msg.ref.Option);
}

impl Opt {
  pub fn new(env: &mut java::Env, addr: i64, num: i64) -> Self {
    static CTOR: java::Constructor<Opt, fn(i64, i64)> = java::Constructor::new();
    CTOR.invoke(env, addr, num)
  }

  pub fn ptr(&self, e: &mut java::Env) -> Ptr {
    static PTR: java::Field<Opt, Ptr> = java::Field::new("ptr");
    PTR.get(e, self)
  }

  pub fn number(&self, env: &mut java::Env) -> OptNumber {
    static NUMBER: java::Field<Opt, i64> = java::Field::new("number");
    OptNumber(NUMBER.get(env, self) as u32)
  }

  pub fn values(&self, env: &mut java::Env) -> Vec<OptValue> {
    static VALUES: java::Method<Opt, fn() -> Vec<OptValue>> = java::Method::new("valueRefs");
    VALUES.invoke(env, self)
  }

  pub fn try_deref(&self,
                   e: &mut java::Env)
                   -> Result<&'static Vec<toad_msg::OptValue<Vec<u8>>>, Throwable> {
    self.ptr(e).addr(e).map(|addr| unsafe {
                         Shared::deref::<Vec<toad_msg::OptValue<Vec<u8>>>>(addr.inner(e)).as_ref()
                                                                                         .unwrap()
                       })
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Option_valueRefs<'local>(mut e: JNIEnv<'local>,
                                                                      o: JObject<'local>)
                                                                      -> jobject {
  let e = &mut e;
  java::lang::Object::from_local(e, o).upcast_to::<Opt>(e)
                                      .try_deref(e)
                                      .map(|values| {
                                        values.iter()
       .map(|v| OptValue::new(e, (v as *const toad_msg::OptValue<Vec<u8>>).addr() as i64))
       .collect::<Vec<_>>()
                                      })
                                      .yield_to_java_or_throw(e)
}
