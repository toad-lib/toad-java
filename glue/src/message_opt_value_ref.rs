use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use jni::JNIEnv;
use toad_jni::Sig;
use toad_msg::{OptNumber, OptValue};

use crate::with_runtime_provenance;

pub struct MessageOptValueRef<'local>(pub JObject<'local>);
impl<'local> MessageOptValueRef<'local> {
  const ID: &'static str = package!(dev.toad.msg.MessageOptionValueRef);
  const CTOR: Sig = Sig::new().arg(Sig::LONG).returning(Sig::VOID);

  pub fn class(env: &mut JNIEnv<'local>) -> JClass<'local> {
    env.find_class(Self::ID).unwrap()
  }

  pub fn new(env: &mut JNIEnv<'local>, addr: i64) -> Self {
    let o = env.new_object(Self::ID, Self::CTOR, &[addr.into()])
               .unwrap();
    Self(o)
  }

  pub unsafe fn ptr<'a>(addr: i64) -> &'a mut OptValue<Vec<u8>> {
    with_runtime_provenance::<OptValue<Vec<u8>>>(addr).as_mut()
                                                      .unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageOptionValueRef_bytes<'local>(mut env: JNIEnv<'local>,
                                                                             _: JClass<'local>,
                                                                             p: i64)
                                                                             -> jobject {
  let val = unsafe { MessageOptValueRef::ptr(p) };
  env.byte_array_from_slice(val.as_bytes()).unwrap().as_raw()
}
