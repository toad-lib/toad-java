use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use jni::JNIEnv;
use toad_jni::java::{self, Object};
use toad_msg::OptNumber;

use super::OptValue;
use crate::mem::SharedMemoryRegion;

pub struct Opt(pub java::lang::Object);

java::object_newtype!(Opt);

impl java::Class for Opt {
  const PATH: &'static str = package!(dev.toad.msg.ref.Option);
}

impl Opt {
  pub fn new(env: &mut java::Env, addr: i64, num: i64) -> Self {
    static CTOR: java::Constructor<Opt, fn(i64, i64)> = java::Constructor::new();
    CTOR.invoke(env, addr, num)
  }

  pub fn number(&self, env: &mut java::Env) -> OptNumber {
    static NUMBER: java::Field<Opt, i64> = java::Field::new("number");
    OptNumber(NUMBER.get(env, self) as u32)
  }

  pub unsafe fn values_ptr<'a>(addr: i64) -> &'a mut Vec<toad_msg::OptValue<Vec<u8>>> {
    crate::mem::Shared::deref::<Vec<toad_msg::OptValue<Vec<u8>>>>(/* TODO */ 0, addr).as_mut()
                                                                                     .unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_Option_number<'local>(mut env: JNIEnv<'local>,
                                                               o: JObject<'local>,
                                                               p: i64)
                                                               -> i64 {
  java::lang::Object::from_local(&mut env, o).upcast_to::<Opt>(&mut env)
                                             .number(&mut env)
                                             .0 as i64
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_msg_Option_values<'local>(mut e: JNIEnv<'local>,
                                                               _: JClass<'local>,
                                                               p: i64)
                                                               -> jobject {
  let o = &unsafe { Opt::values_ptr(p) };

  let refs = o.iter()
              .map(|v| OptValue::new(&mut e, (&v.0 as *const Vec<u8>).addr() as i64))
              .collect::<Vec<_>>();

  refs.yield_to_java(&mut e)
}