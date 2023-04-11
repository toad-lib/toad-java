use toad_jni::java;

pub struct Code(java::lang::Object);

java::object_newtype!(Code);
impl java::Class for Code {
  const PATH: &'static str = package!(dev.toad.msg.Code);
}

impl Code {
  pub fn from_toad(e: &mut java::Env, code: toad_msg::Code) -> Self {
    static CTOR: java::Constructor<Code, fn(i16, i16)> = java::Constructor::new();
    CTOR.invoke(e, code.class.into(), code.detail.into())
  }

  pub fn to_toad(&self, e: &mut java::Env) -> toad_msg::Code {
    static CLASS: java::Method<Code, fn() -> i16> = java::Method::new("codeClass");
    static DETAIL: java::Method<Code, fn() -> i16> = java::Method::new("codeDetail");

    toad_msg::Code::new(CLASS.invoke(e, self) as u8, DETAIL.invoke(e, self) as u8)
  }
}
