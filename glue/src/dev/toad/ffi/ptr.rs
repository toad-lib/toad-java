use toad_jni::java::lang::{Long, Throwable};
use toad_jni::java::{self};

pub struct Ptr(java::lang::Object);
java::object_newtype!(Ptr);
impl java::Class for Ptr {
  const PATH: &'static str = package!(dev.toad.ffi.Ptr);
}

impl Ptr {
  pub fn addr(&self, e: &mut java::Env) -> Result<Long, Throwable> {
    static ADDR: java::Method<Ptr, fn() -> Result<Long, Throwable>> = java::Method::new("addr");
    ADDR.invoke(e, self)
  }
}
