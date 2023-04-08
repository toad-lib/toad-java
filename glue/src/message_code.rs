use toad_jni::java;
use toad_msg::Code;

pub struct MessageCode(java::lang::Object);

java::object_newtype!(MessageCode);
impl java::Class for MessageCode {
  const PATH: &'static str = package!(dev.toad.msg.MessageCode);
}

impl MessageCode {
  pub fn new(e: &mut java::Env, code: Code) -> Self {
    static CTOR: java::Constructor<MessageCode, fn(i32, i32)> = java::Constructor::new();
    CTOR.invoke(e, code.class.into(), code.detail.into())
  }
}
