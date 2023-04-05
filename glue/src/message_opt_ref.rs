use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use jni::JNIEnv;
use toad_jni::Sig;
use toad_msg::{OptNumber, OptValue};

use crate::message_opt_value_ref::MessageOptValueRef;
use crate::with_runtime_provenance;

pub struct MessageOptRef<'local>(pub JObject<'local>);
impl<'local> MessageOptRef<'local> {
  const ID: &'static str = package!(dev.toad.msg.MessageOptionRef);
  const CTOR: Sig = Sig::new().arg(Sig::LONG)
                              .arg(Sig::LONG)
                              .returning(Sig::VOID);

  const NUMBER: &'static str = "number";

  pub fn class(env: &mut JNIEnv<'local>) -> JClass<'local> {
    env.find_class(Self::ID).unwrap()
  }

  pub fn new(env: &mut JNIEnv<'local>, addr: i64, num: i64) -> Self {
    let o = env.new_object(Self::ID, Self::CTOR, &[addr.into(), num.into()])
               .unwrap();
    Self(o)
  }

  pub fn number(&self, env: &mut JNIEnv<'local>) -> OptNumber {
    OptNumber(env.get_field(&self.0, Self::NUMBER, Sig::LONG)
                 .unwrap()
                 .j()
                 .unwrap() as u32)
  }

  pub unsafe fn values_ptr<'a>(addr: i64) -> &'a mut Vec<OptValue<Vec<u8>>> {
    with_runtime_provenance::<Vec<OptValue<Vec<u8>>>>(addr).as_mut()
                                                           .unwrap()
  }
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageOptionRef_number<'local>(mut env: JNIEnv<'local>,
                                                                                o: JObject<'local>,
                                                                                p: i64)
                                                                                -> i64 {
  MessageOptRef(o).number(&mut env).0 as i64
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageOptionRef_values<'local>(mut env: JNIEnv<'local>,
                                                                                _: JClass<'local>,
                                                                                p: i64)
                                                                                -> jobject {
  let o = &MessageOptRef::values_ptr(p);

  let value_ref_class = MessageOptValueRef::class(&mut env);
  let mut arr = env.new_object_array(o.len() as i32, value_ref_class, JObject::null())
                   .unwrap();

  for (ix, v) in o.iter().enumerate() {
    let value_ref = MessageOptValueRef::new(&mut env, v as *const _ as i64);
    env.set_object_array_element(&mut arr, ix as i32, value_ref.0);
  }

  arr.as_raw()
}
