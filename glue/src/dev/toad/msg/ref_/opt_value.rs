use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use toad_jni::java::lang::Throwable;
use toad_jni::java::{self, ResultYieldToJavaOrThrow};

use crate::dev::toad::ffi::Ptr;
use crate::mem::{Shared, SharedMemoryRegion};

pub struct OptValue(java::lang::Object);

java::object_newtype!(OptValue);
impl java::Class for OptValue {
  const PATH: &'static str = package!(dev.toad.msg.ref.OptionValue);
}

impl OptValue {
  pub fn new(env: &mut java::Env, addr: i64) -> Self {
    static CTOR: java::Constructor<OptValue, fn(i64)> = java::Constructor::new();
    CTOR.invoke(env, addr)
  }

  pub fn ptr(&self, e: &mut java::Env) -> Ptr {
    static PTR: java::Field<OptValue, Ptr> = java::Field::new("ptr");
    PTR.get(e, self)
  }

  pub fn try_deref(&self,
                   e: &mut java::Env)
                   -> Result<&'static toad_msg::OptValue<Vec<u8>>, Throwable> {
    self.ptr(e).addr(e).map(|addr| unsafe {
                         Shared::deref::<toad_msg::OptValue<Vec<u8>>>(addr.inner(e)).as_ref()
                                                                                    .unwrap()
                       })
  }

  pub fn bytes(&self, env: &mut java::Env) -> Vec<u8> {
    static AS_BYTES: java::Method<OptValue, fn() -> Vec<i8>> = java::Method::new("asBytes");
    AS_BYTES.invoke(env, self)
            .into_iter()
            .map(|i| u8::from_be_bytes(i.to_be_bytes()))
            .collect()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_OptionValue_asBytes<'local>(mut env: java::Env<'local>,
                                                                         val: JObject<'local>)
                                                                         -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, val).upcast_to::<OptValue>(e)
                                        .try_deref(e)
                                        .map(|val| {
                                          let arr =
                                            e.byte_array_from_slice(val.as_bytes()).unwrap();
                                          java::lang::Object::from_local(e, arr)
                                        })
                                        .yield_to_java_or_throw(e)
}
