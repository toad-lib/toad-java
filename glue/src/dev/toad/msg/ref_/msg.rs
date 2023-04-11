use jni::objects::JClass;
use jni::sys::jobject;
use toad_jni::java::{self, Object};

use crate::dev::toad::msg::ref_::Opt;
use crate::dev::toad::msg::{Code, Type};
use crate::mem::{Shared, SharedMemoryRegion};

pub struct Message(java::lang::Object);

java::object_newtype!(Message);
impl java::Class for Message {
  const PATH: &'static str = package!(dev.toad.msg.ref.Message);
}

impl Message {
  pub fn new(env: &mut java::Env, message: toad_msg::alloc::Message) -> Self {
    let ptr = unsafe { Shared::alloc_message(message) };
    static CTOR: java::Constructor<Message, fn(i64)> = java::Constructor::new();
    CTOR.invoke(env, ptr.addr() as i64)
  }

  pub fn close(&self, env: &mut java::Env) {
    static CLOSE: java::Method<Message, fn()> = java::Method::new("close");
    CLOSE.invoke(env, self)
  }

  pub fn ty(&self, env: &mut java::Env) -> toad_msg::Type {
    static TYPE: java::Method<Message, fn() -> Type> = java::Method::new("type");
    TYPE.invoke(env, self).to_toad(env)
  }

  pub unsafe fn ptr<'a>(addr: i64) -> &'a mut toad_msg::alloc::Message {
    crate::mem::Shared::deref::<toad_msg::alloc::Message>(/* TODO */ 0, addr).as_mut()
                                                                             .unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_id<'local>(mut env: java::Env<'local>,
                                                                _: JClass<'local>,
                                                                addr: i64)
                                                                -> i32 {
  let msg = unsafe { Message::ptr(addr) };
  msg.id.0 as i32
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_token<'local>(mut env: java::Env<'local>,
                                                                   _: JClass<'local>,
                                                                   addr: i64)
                                                                   -> jobject {
  let msg = unsafe { Message::ptr(addr) };
  env.byte_array_from_slice(&msg.token.0).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_payload<'local>(mut env: java::Env<'local>,
                                                                     _: JClass<'local>,
                                                                     addr: i64)
                                                                     -> jobject {
  let msg = unsafe { Message::ptr(addr) };
  env.byte_array_from_slice(&msg.payload.0).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_type<'local>(mut e: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe { Message::ptr(addr) };
  Type::new(&mut e, msg.ty).yield_to_java(&mut e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_code<'local>(mut e: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe { Message::ptr(addr) };
  Code::new(&mut e, msg.code).yield_to_java(&mut e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_opts<'local>(mut e: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe { Message::ptr(addr) };
  let opts = &msg.opts;

  let refs = opts.into_iter()
                 .map(|(n, v)| Opt::new(&mut e, v as *const _ as i64, n.0.into()))
                 .collect::<Vec<_>>();

  refs.yield_to_java(&mut e)
}
