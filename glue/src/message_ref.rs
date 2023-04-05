use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use jni::JNIEnv;
use toad_jni::Sig;
use toad_msg::alloc::Message;

use crate::message_code::MessageCode;
use crate::message_opt_ref::MessageOptRef;
use crate::message_type::MessageType;
use crate::{with_runtime_provenance, RUNTIME};

pub struct MessageRef<'local>(pub JObject<'local>);
impl<'local> MessageRef<'local> {
  const ID: &'static str = package!(dev.toad.msg.MessageRef);
  const CTOR: Sig = Sig::new().arg(Sig::LONG).returning(Sig::VOID);

  pub fn new(env: &mut JNIEnv<'local>, addr: i64) -> Self {
    let o = env.new_object(Self::ID, Self::CTOR, &[addr.into()])
               .unwrap();
    Self(o)
  }

  pub unsafe fn ptr<'a>(addr: i64) -> &'a mut Message {
    with_runtime_provenance::<Message>(addr).as_mut().unwrap()
  }
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageRef_id<'local>(mut env: JNIEnv<'local>,
                                                                      _: JClass<'local>,
                                                                      addr: i64)
                                                                      -> i32 {
  MessageRef::ptr(addr).id.0 as i32
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageRef_token<'local>(mut env: JNIEnv<'local>,
                                                                         _: JClass<'local>,
                                                                         addr: i64)
                                                                         -> jobject {
  env.byte_array_from_slice(&MessageRef::ptr(addr).token.0)
     .unwrap()
     .as_raw()
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageRef_payload<'local>(mut env: JNIEnv<'local>,
                                                                           _: JClass<'local>,
                                                                           addr: i64)
                                                                           -> jobject {
  env.byte_array_from_slice(&MessageRef::ptr(addr).payload.0)
     .unwrap()
     .as_raw()
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageRef_type<'local>(mut env: JNIEnv<'local>,
                                                                        _: JClass<'local>,
                                                                        addr: i64)
                                                                        -> jobject {
  MessageType::new(&mut env, MessageRef::ptr(addr).ty).0
                                                      .into_raw()
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageRef_code<'local>(mut env: JNIEnv<'local>,
                                                                        _: JClass<'local>,
                                                                        addr: i64)
                                                                        -> jobject {
  MessageCode::new(&mut env, MessageRef::ptr(addr).code).0
                                                        .into_raw()
}

#[no_mangle]
pub unsafe extern "system" fn Java_dev_toad_msg_MessageRef_opts<'local>(mut env: JNIEnv<'local>,
                                                                        _: JClass<'local>,
                                                                        addr: i64)
                                                                        -> jobject {
  let opts = &MessageRef::ptr(addr).opts;

  let opt_ref_class = MessageOptRef::class(&mut env);
  let mut arr = env.new_object_array((*opts).len() as i32, opt_ref_class, JObject::null())
                   .unwrap();

  for (ix, (num, v)) in (*opts).iter().enumerate() {
    let opt_ref = MessageOptRef::new(&mut env, v as *const _ as i64, num.0.into());
    env.set_object_array_element(&mut arr, ix as i32, opt_ref.0);
  }

  arr.as_raw()
}
