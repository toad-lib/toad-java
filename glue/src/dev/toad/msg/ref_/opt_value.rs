use jni::objects::JClass;
use jni::sys::jobject;
use toad_jni::java;

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

  pub fn bytes(&self, env: &mut java::Env) -> Vec<u8> {
    static AS_BYTES: java::Method<OptValue, fn() -> Vec<i8>> = java::Method::new("asBytes");
    AS_BYTES.invoke(env, self)
            .into_iter()
            .map(|i| u8::from_be_bytes(i.to_be_bytes()))
            .collect()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_OptionValue_bytes<'local>(mut env: java::Env<'local>,
                                                                       _: JClass<'local>,
                                                                       p: i64)
                                                                       -> jobject {
  let val = unsafe {
    Shared::deref::<toad_msg::OptValue<Vec<u8>>>(p).as_ref()
                                                   .unwrap()
  };
  env.byte_array_from_slice(val.as_bytes()).unwrap().as_raw()
}
