use jni::objects::JClass;
use jni::sys::jobject;
use toad_jni::java;
use toad_msg::OptValue;

use crate::mem::RuntimeAllocator;

pub struct MessageOptValueRef(java::lang::Object);

java::object_newtype!(MessageOptValueRef);
impl java::Class for MessageOptValueRef {
  const PATH: &'static str = package!(dev.toad.msg.MessageOptionValueRef);
}

impl MessageOptValueRef {
  pub fn new(env: &mut java::Env, addr: i64) -> Self {
    static CTOR: java::Constructor<MessageOptValueRef, fn(i64)> = java::Constructor::new();
    CTOR.invoke(env, addr)
  }

  pub unsafe fn ptr<'a>(addr: i64) -> &'a mut OptValue<Vec<u8>> {
    crate::mem::Runtime::deref_inner::<OptValue<Vec<u8>>>(/* TODO */ 0, addr).as_mut()
                                                                             .unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageOptionValueRef_bytes<'local>(mut env: java::Env<'local>,
                                                                             _: JClass<'local>,
                                                                             p: i64)
                                                                             -> jobject {
  let val = unsafe { MessageOptValueRef::ptr(p) };
  env.byte_array_from_slice(val.as_bytes()).unwrap().as_raw()
}
