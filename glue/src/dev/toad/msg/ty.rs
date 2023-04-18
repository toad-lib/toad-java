use toad_jni::java;

pub struct Type(java::lang::Object);

java::object_newtype!(Type);
impl java::Class for Type {
  const PATH: &'static str = package!(dev.toad.msg.Type);
}

impl Type {
  pub fn from_toad(env: &mut java::Env, ty: toad_msg::Type) -> Self {
    static FROM_STRING: java::StaticMethod<Type, fn(String) -> Type> =
      java::StaticMethod::new("fromString");

    let str = match ty {
      | toad_msg::Type::Con => "CON",
      | toad_msg::Type::Non => "NON",
      | toad_msg::Type::Ack => "ACK",
      | toad_msg::Type::Reset => "RESET",
    };

    FROM_STRING.invoke(env, str.to_string())
  }

  pub fn to_toad(&self, env: &mut java::Env) -> toad_msg::Type {
    static TO_STRING: java::Method<Type, fn() -> String> = java::Method::new("toString");

    match TO_STRING.invoke(env, self).trim().to_uppercase().as_str() {
      | "CON" => toad_msg::Type::Con,
      | "NON" => toad_msg::Type::Non,
      | "ACK" => toad_msg::Type::Ack,
      | "RESET" => toad_msg::Type::Reset,
      | o => panic!("malformed message type {}", o),
    }
  }
}
