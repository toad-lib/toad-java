use std::collections::BTreeMap;

use jni::objects::{JClass, JObject, JThrowable};
use jni::sys::jobject;
use toad::net::Addrd;
use toad_jni::java::lang::Throwable;
use toad_jni::java::net::InetSocketAddress;
use toad_jni::java::{self, Object};

use crate::dev::toad::ffi::Ptr;
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

  pub fn ptr(&self, e: &mut java::Env) -> Ptr {
    static PTR: java::Field<Message, Ptr> = java::Field::new("ptr");
    PTR.get(e, self)
  }

  pub fn toad_ref(&self,
                  e: &mut java::Env)
                  -> Result<&'static Addrd<toad_msg::alloc::Message>, Throwable> {
    self.ptr(e).addr(e).map(|addr| unsafe {
                         Shared::deref::<Addrd<toad_msg::alloc::Message>>(addr.inner(e)).as_ref()
                                                                                        .unwrap()
                       })
  }

  pub fn to_toad(&self, env: &mut java::Env) -> Addrd<toad_msg::alloc::Message> {
    let msg = toad_msg::alloc::Message { ty: self.ty(env),
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
                                                            Vec<toad_msg::OptValue<Vec<u8>>>>>() };
    Addrd(msg,
          self.addr(env)
              .expect("java should have made sure the address was present"))
  }

  pub fn close(&self, env: &mut java::Env) {
    static CLOSE: java::Method<Message, fn()> = java::Method::new("close");
    CLOSE.invoke(env, self)
  }

  pub fn addr(&self, env: &mut java::Env) -> Option<no_std_net::SocketAddr> {
    static SOURCE: java::Method<Message, fn() -> java::util::Optional<InetSocketAddress>> =
      java::Method::new("addr");
    SOURCE.invoke(env, self)
          .to_option(env)
          .map(|a| a.to_no_std(env))
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
                                                                msg: JObject<'local>)
                                                                -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| Id::from_toad(e, msg.data().id).yield_to_java(e))
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_token<'local>(mut env: java::Env<'local>,
                                                                   msg: JObject<'local>)
                                                                   -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| {
                                          Token::from_toad(e, msg.data().token).yield_to_java(e)
                                        })
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_payloadBytes<'local>(mut env: java::Env<'local>,
                                                                          msg: JObject<'local>)
                                                                          -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| {
                                          e.byte_array_from_slice(&msg.data().payload.0)
                                           .unwrap()
                                           .as_raw()
                                        })
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_type<'local>(mut env: java::Env<'local>,
                                                                  msg: JObject<'local>)
                                                                  -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| {
                                          Type::from_toad(e, msg.data().ty).yield_to_java(e)
                                        })
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_code<'local>(mut env: java::Env<'local>,
                                                                  msg: JObject<'local>)
                                                                  -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| {
                                          Code::from_toad(e, msg.data().code).yield_to_java(e)
                                        })
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_addr<'local>(mut env: java::Env<'local>,
                                                                  msg: JObject<'local>)
                                                                  -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| {
                                          let addr = InetSocketAddress::from_no_std(e, msg.addr());
                                          java::util::Optional::of(e, addr).yield_to_java(e)
                                        })
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_ref_Message_optionRefs<'local>(mut env: java::Env<'local>,
                                                                        msg: JObject<'local>)
                                                                        -> jobject {
  let e = &mut env;
  java::lang::Object::from_local(e, msg).upcast_to::<Message>(e)
                                        .toad_ref(e)
                                        .map(|msg| {
                                          let opts = &msg.data().opts;

                                          let refs =
                                            opts.into_iter()
                                                .map(|(n, v)| {
                                                  Opt::new(e, v as *const _ as i64, n.0.into())
                                                })
                                                .collect::<Vec<_>>();

                                          refs.yield_to_java(e)
                                        })
                                        .map_err(|err| {
                                          let err = JThrowable::from(err.downcast(e).to_local(e));
                                          e.throw(err).unwrap()
                                        })
                                        .unwrap_or(*JObject::null())
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

    let ptr: *mut Addrd<toad_msg::alloc::Message> =
      Box::into_raw(Box::new(Addrd(toad_msg, "127.0.0.1:1234".parse().unwrap())));

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

    let ptr: *mut Addrd<toad_msg::alloc::Message> =
      Box::into_raw(Box::new(Addrd(toad_msg, "127.0.0.1:1234".parse().unwrap())));

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
