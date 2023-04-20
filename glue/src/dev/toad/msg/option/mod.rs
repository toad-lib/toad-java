use toad_jni::java;

pub struct ContentFormat(java::lang::Object);
java::object_newtype!(ContentFormat);
impl java::Class for ContentFormat {
  const PATH: &'static str = package!(dev.toad.msg.option.ContentFormat);
}

impl ContentFormat {
  pub fn new(e: &mut java::Env, o: super::Option) -> Self {
    static CTOR: java::Constructor<ContentFormat, fn(super::Option)> = java::Constructor::new();
    CTOR.invoke(e, o)
  }
}
