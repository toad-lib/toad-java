use std::collections::BTreeMap;

use jni::objects::JObject;
use jni::sys::{jbyteArray, jobject};
use toad_jni::java::net::InetSocketAddress;
use toad_jni::java::util::ArrayList;
use toad_jni::java::{self};
use toad_msg::{OptNumber, TryIntoBytes};

use crate::dev::toad::msg::owned::Opt;
use crate::dev::toad::msg::{Code, Id, Token, Type};

pub struct Message(java::lang::Object);

java::object_newtype!(Message);
impl java::Class for Message {
  const PATH: &'static str = package!(dev.toad.msg.owned.Message);
}

impl Message {
  pub fn id(&self, e: &mut java::Env) -> Id {
    static ID: java::Field<Message, Id> = java::Field::new("id");
    ID.get(e, self)
  }

  pub fn token(&self, e: &mut java::Env) -> Token {
    static TOKEN: java::Field<Message, Token> = java::Field::new("token");
    TOKEN.get(e, self)
  }

  pub fn ty(&self, e: &mut java::Env) -> Type {
    static TY: java::Field<Message, Type> = java::Field::new("type");
    TY.get(e, self)
  }

  pub fn code(&self, e: &mut java::Env) -> Code {
    static CODE: java::Field<Message, Code> = java::Field::new("code");
    CODE.get(e, self)
  }

  pub fn options(&self, e: &mut java::Env) -> Vec<Opt> {
    static OPTIONS: java::Field<Message, ArrayList<Opt>> = java::Field::new("opts");
    OPTIONS.get(e, self).into_iter().collect()
  }

  pub fn payload(&self, e: &mut java::Env) -> Vec<u8> {
    static PAYLOAD: java::Field<Message, Vec<i8>> = java::Field::new("payload");
    PAYLOAD.get(e, self)
           .into_iter()
           .map(|i| u8::from_be_bytes(i.to_be_bytes()))
           .collect()
  }

  pub fn addr(&self, e: &mut java::Env) -> Option<InetSocketAddress> {
    static ADDR: java::Field<Message, java::util::Optional<InetSocketAddress>> =
      java::Field::new("addr");
    ADDR.get(e, self).to_option(e)
  }

  pub fn to_toad(&self, e: &mut java::Env) -> toad_msg::alloc::Message {
    toad_msg::Message { id: self.id(e).to_toad(e),
                        ty: self.ty(e).to_toad(e),
                        ver: Default::default(),
                        token: self.token(e).to_toad(e),
                        code: self.code(e).to_toad(e),
                        opts:
                          self.options(e)
                              .into_iter()
                              .map(|opt| {
                                (opt.number(e),
                                 opt.values(e)
                                    .into_iter()
                                    .map(|v| toad_msg::OptValue(v.bytes(e)))
                                    .collect())
                              })
                              .collect::<BTreeMap<OptNumber, Vec<toad_msg::OptValue<Vec<u8>>>>>(),
                        payload: toad_msg::Payload(self.payload(e)) }
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_owned_Message_toBytes<'local>(mut env: java::Env<'local>,
                                                                       msg: JObject<'local>)
                                                                       -> jbyteArray {
  let jmsg = java::lang::Object::from_local(&mut env, msg).upcast_to::<Message>(&mut env);
  let message = jmsg.to_toad(&mut env);
  let bytes = message.try_into_bytes::<Vec<u8>>().unwrap();

  env.byte_array_from_slice(&bytes).unwrap().as_raw()
}
