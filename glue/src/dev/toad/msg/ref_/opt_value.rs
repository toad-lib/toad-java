use jni::objects::JClass;
use jni::sys::jobject;
use toad_jni::java;

use crate::mem::SharedMemoryRegion;

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

  pub unsafe fn ptr<'a>(addr: i64) -> &'a mut toad_msg::OptValue<Vec<u8>> {
    crate::mem::Shared::deref::<toad_msg::OptValue<Vec<u8>>>(/* TODO */ 0, addr).as_mut()
                                                                                .unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_OptionValue_bytes<'local>(mut env: java::Env<'local>,
                                                                       _: JClass<'local>,
                                                                       p: i64)
                                                                       -> jobject {
  let val = unsafe { OptValue::ptr(p) };
  env.byte_array_from_slice(val.as_bytes()).unwrap().as_raw()
}
