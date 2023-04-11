use toad_jni::java;

pub struct Code(java::lang::Object);

java::object_newtype!(Code);
impl java::Class for Code {
  const PATH: &'static str = package!(dev.toad.msg.Code);
}

impl Code {
  pub fn new(e: &mut java::Env, code: toad_msg::Code) -> Self {
    static CTOR: java::Constructor<Code, fn(i32, i32)> = java::Constructor::new();
    CTOR.invoke(e, code.class.into(), code.detail.into())
  }
}
