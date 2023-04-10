use jni::objects::JClass;
use jni::sys::jobject;
use toad_jni::java::{self, Object};
use toad_msg::alloc::Message;
use toad_msg::Type;

use crate::mem::{Shared, SharedMemoryRegion};
use crate::message_code::MessageCode;
use crate::message_opt_ref::MessageOptRef;
use crate::message_type::MessageType;

pub struct MessageRef(java::lang::Object);

java::object_newtype!(MessageRef);
impl java::Class for MessageRef {
  const PATH: &'static str = package!(dev.toad.msg.MessageRef);
}

impl MessageRef {
  pub fn new(env: &mut java::Env, message: Message) -> Self {
    let ptr = unsafe { Shared::alloc_message(message) };
    static CTOR: java::Constructor<MessageRef, fn(i64)> = java::Constructor::new();
    CTOR.invoke(env, ptr.addr() as i64)
  }

  pub fn close(&self, env: &mut java::Env) {
    static CLOSE: java::Method<MessageRef, fn()> = java::Method::new("close");
    CLOSE.invoke(env, self)
  }

  pub fn ty(&self, env: &mut java::Env) -> Type {
    static TYPE: java::Method<MessageRef, fn() -> MessageType> = java::Method::new("type");
    TYPE.invoke(env, self).to_toad(env)
  }

  pub unsafe fn ptr<'a>(addr: i64) -> &'a mut Message {
    crate::mem::Shared::deref::<Message>(/* TODO */ 0, addr).as_mut()
                                                            .unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageRef_id<'local>(mut env: java::Env<'local>,
                                                               _: JClass<'local>,
                                                               addr: i64)
                                                               -> i32 {
  let msg = unsafe { MessageRef::ptr(addr) };
  msg.id.0 as i32
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageRef_token<'local>(mut env: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe { MessageRef::ptr(addr) };
  env.byte_array_from_slice(&msg.token.0).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageRef_payload<'local>(mut env: java::Env<'local>,
                                                                    _: JClass<'local>,
                                                                    addr: i64)
                                                                    -> jobject {
  let msg = unsafe { MessageRef::ptr(addr) };
  env.byte_array_from_slice(&msg.payload.0).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageRef_type<'local>(mut e: java::Env<'local>,
                                                                 _: JClass<'local>,
                                                                 addr: i64)
                                                                 -> jobject {
  let msg = unsafe { MessageRef::ptr(addr) };
  MessageType::new(&mut e, msg.ty).yield_to_java(&mut e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageRef_code<'local>(mut e: java::Env<'local>,
                                                                 _: JClass<'local>,
                                                                 addr: i64)
                                                                 -> jobject {
  let msg = unsafe { MessageRef::ptr(addr) };
  MessageCode::new(&mut e, msg.code).yield_to_java(&mut e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_MessageRef_opts<'local>(mut e: java::Env<'local>,
                                                                 _: JClass<'local>,
                                                                 addr: i64)
                                                                 -> jobject {
  let msg = unsafe { MessageRef::ptr(addr) };
  let opts = &msg.opts;

  let refs = opts.into_iter()
                 .map(|(n, v)| MessageOptRef::new(&mut e, v as *const _ as i64, n.0.into()))
                 .collect::<Vec<_>>();

  refs.yield_to_java(&mut e)
}
