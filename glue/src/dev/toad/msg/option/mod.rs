use toad_jni::java;

use super::owned::Opt;

pub struct ContentFormat(java::lang::Object);
java::object_newtype!(ContentFormat);
impl java::Class for ContentFormat {
  const PATH: &'static str = package!(dev.toad.msg.option.ContentFormat);
}

impl ContentFormat {
  pub fn new(e: &mut java::Env, o: Opt) -> Self {
    static CTOR: java::Constructor<ContentFormat, fn(Opt)> = java::Constructor::new();
    CTOR.invoke(e, o)
  }
}
