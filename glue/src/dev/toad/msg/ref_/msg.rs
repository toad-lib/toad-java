use std::collections::BTreeMap;

use jni::objects::JClass;
use jni::sys::jobject;
use toad_jni::java::{self, Object};

use crate::dev::toad::msg::ref_::Opt;
use crate::dev::toad::msg::{Code, Id, Token, Type};
use crate::mem::{Shared, SharedMemoryRegion};

pub struct Message(java::lang::Object);

java::object_newtype!(Message);
impl java::Class for Message {
  const PATH: &'static str = package!(dev.toad.msg.ref.Message);
}

impl Message {
  pub fn new(env: &mut java::Env, msg_addr: i64) -> Self {
    static CTOR: java::Constructor<Message, fn(i64)> = java::Constructor::new();
    CTOR.invoke(env, msg_addr)
  }

  pub fn to_toad(&self, env: &mut java::Env) -> toad_msg::alloc::Message {
    toad_msg::alloc::Message { ty: self.ty(env),
                               ver: toad_msg::Version::default(),
                               code: self.code(env),
                               id: self.id(env),
                               token: self.token(env),
                               payload: toad_msg::Payload(self.payload(env)),
                               opts: self.options(env)
                                         .into_iter()
                                         .map(|opt| {
                                           (opt.number(env),
                                            opt.values(env)
                                               .into_iter()
                                               .map(|v| toad_msg::OptValue(v.bytes(env)))
                                               .collect())
                                         })
                                         .collect::<BTreeMap<toad_msg::OptNumber,
                                                  Vec<toad_msg::OptValue<Vec<u8>>>>>() }
  }

  pub fn close(&self, env: &mut java::Env) {
    static CLOSE: java::Method<Message, fn()> = java::Method::new("close");
    CLOSE.invoke(env, self)
  }

  pub fn ty(&self, env: &mut java::Env) -> toad_msg::Type {
    static TYPE: java::Method<Message, fn() -> Type> = java::Method::new("type");
    TYPE.invoke(env, self).to_toad(env)
  }

  pub fn id(&self, env: &mut java::Env) -> toad_msg::Id {
    static ID: java::Method<Message, fn() -> Id> = java::Method::new("id");
    ID.invoke(env, self).to_toad(env)
  }

  pub fn token(&self, env: &mut java::Env) -> toad_msg::Token {
    static TOKEN: java::Method<Message, fn() -> Token> = java::Method::new("token");
    TOKEN.invoke(env, self).to_toad(env)
  }

  pub fn code(&self, env: &mut java::Env) -> toad_msg::Code {
    static CODE: java::Method<Message, fn() -> Code> = java::Method::new("code");
    CODE.invoke(env, self).to_toad(env)
  }

  pub fn options(&self, env: &mut java::Env) -> Vec<Opt> {
    static OPTIONS: java::Method<Message, fn() -> Vec<Opt>> = java::Method::new("optionRefs");
    OPTIONS.invoke(env, self)
  }

  pub fn payload(&self, env: &mut java::Env) -> Vec<u8> {
    static PAYLOAD: java::Method<Message, fn() -> Vec<i8>> = java::Method::new("payloadBytes");
    PAYLOAD.invoke(env, self)
           .into_iter()
           .map(|i| u8::from_be_bytes(i.to_be_bytes()))
           .collect()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_id<'local>(mut env: java::Env<'local>,
                                                                _: JClass<'local>,
                                                                addr: i64)
                                                                -> jobject {
  let e = &mut env;
  let msg = unsafe {
    Shared::deref::<toad_msg::alloc::Message>(addr).as_ref()
                                                   .unwrap()
  };
  Id::from_toad(e, msg.id).yield_to_java(e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_token<'local>(mut env: java::Env<'local>,
                                                                   _: JClass<'local>,
                                                                   addr: i64)
                                                                   -> jobject {
  let e = &mut env;
  let msg = unsafe {
    Shared::deref::<toad_msg::alloc::Message>(addr).as_ref()
                                                   .unwrap()
  };
  Token::from_toad(e, msg.token).yield_to_java(e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_payload<'local>(mut env: java::Env<'local>,
                                                                     _: JClass<'local>,
                                                                     addr: i64)
                                                                     -> jobject {
  let msg = unsafe {
    Shared::deref::<toad_msg::alloc::Message>(addr).as_ref()
                                                   .unwrap()
  };
  env.byte_array_from_slice(&msg.payload.0).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_type<'local>(mut e: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe {
    Shared::deref::<toad_msg::alloc::Message>(addr).as_ref()
                                                   .unwrap()
  };
  Type::new(&mut e, msg.ty).yield_to_java(&mut e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_code<'local>(mut e: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe {
    Shared::deref::<toad_msg::alloc::Message>(addr).as_ref()
                                                   .unwrap()
  };
  Code::from_toad(&mut e, msg.code).yield_to_java(&mut e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_opts<'local>(mut e: java::Env<'local>,
                                                                  _: JClass<'local>,
                                                                  addr: i64)
                                                                  -> jobject {
  let msg = unsafe {
    Shared::deref::<toad_msg::alloc::Message>(addr).as_ref()
                                                   .unwrap()
  };
  let opts = &msg.opts;

  let refs = opts.into_iter()
                 .map(|(n, v)| Opt::new(&mut e, v as *const _ as i64, n.0.into()))
                 .collect::<Vec<_>>();

  refs.yield_to_java(&mut e)
}

#[cfg(test)]
mod tests {
  use toad_jni::java::Signature;
  use toad_msg::{MessageOptions, Payload};

  use super::*;

  #[test]
  fn roundtrip() {
    let mut env = crate::test::init();
    let e = &mut env;

    let mut toad_msg = {
      use tinyvec::array_vec;
      use toad_msg::*;

      Message::new(Type::Con, Code::GET, Id(333), Token(array_vec![1, 2, 3, 4]))
    };

    toad_msg.set_path("foo/bar/baz").ok();
    toad_msg.set_payload(Payload(r#"{"id": 123, "stuff": ["abc"]}"#.as_bytes().to_vec()));

    let ptr: *mut toad_msg::alloc::Message = Box::into_raw(Box::new(toad_msg));

    let msg = Message::new(e, ptr.addr() as i64);

    assert_eq!(&msg.to_toad(e), unsafe { ptr.as_ref().unwrap() });
  }

  #[test]
  fn message_ref_should_throw_when_used_after_close() {
    let mut env = crate::test::init();
    let e = &mut env;

    let mut toad_msg = {
      use tinyvec::array_vec;
      use toad_msg::*;

      Message::new(Type::Con, Code::GET, Id(333), Token(array_vec![1, 2, 3, 4]))
    };

    toad_msg.set_path("foo/bar/baz").ok();
    toad_msg.set_payload(Payload(r#"{"id": 123, "stuff": ["abc"]}"#.as_bytes().to_vec()));

    let ptr: *mut toad_msg::alloc::Message = Box::into_raw(Box::new(toad_msg));

    let msg = Message::new(e, ptr.addr() as i64);

    assert_eq!(msg.ty(e), toad_msg::Type::Con);
    msg.close(e);

    let msg_o = msg.downcast(e);
    e.call_method(msg_o.as_local(),
                  "type",
                  Signature::of::<fn() -> Type>(),
                  &[])
     .ok();

    let err = e.exception_occurred().unwrap();
    e.exception_clear().unwrap();
    assert!(e.is_instance_of(err, concat!(package!(dev.toad.ffi.Ptr), "$ExpiredError"))
             .unwrap());
  }
}
