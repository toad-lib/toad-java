use toad_jni::java;
use toad_msg::Type;

pub struct MessageType(java::lang::Object);

java::object_newtype!(MessageType);
impl java::Class for MessageType {
  const PATH: &'static str = package!(dev.toad.msg.MessageType);
}

impl MessageType {
  pub fn new(env: &mut java::Env, ty: Type) -> Self {
    static FROM_STRING: java::StaticMethod<MessageType, fn(String) -> MessageType> =
      java::StaticMethod::new("fromString");

    let str = match ty {
      | Type::Con => "CON",
      | Type::Non => "NON",
      | Type::Ack => "ACK",
      | Type::Reset => "RESET",
    };

    FROM_STRING.invoke(env, str.to_string())
  }

  pub fn to_toad(&self, env: &mut java::Env) -> Type {
    static TO_STRING: java::Method<MessageType, fn() -> String> = java::Method::new("toString");

    match TO_STRING.invoke(env, self).trim().to_uppercase().as_str() {
      | "CON" => Type::Con,
      | "NON" => Type::Non,
      | "ACK" => Type::Ack,
      | "RESET" => Type::Reset,
      | o => panic!("malformed message type {}", o),
    }
  }
}
