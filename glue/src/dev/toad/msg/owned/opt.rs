use toad_jni::java::util::ArrayList;
use toad_jni::java::{self};
use toad_msg::OptNumber;

use super::OptValue;

pub struct Opt(java::lang::Object);

java::object_newtype!(Opt);
impl java::Class for Opt {
  const PATH: &'static str = package!(dev.toad.msg.owned.Option);
}

impl Opt {
  pub fn new(e: &mut java::Env, num: i64, vals: ArrayList<OptValue>) -> Self {
    static CTOR: java::Constructor<Opt, fn(i64, ArrayList<OptValue>)> = java::Constructor::new();
    CTOR.invoke(e, num, vals)
  }

  pub fn number(&self, e: &mut java::Env) -> OptNumber {
    static NUMBER: java::Field<Opt, crate::dev::toad::ffi::u32> = java::Field::new("number");
    OptNumber(NUMBER.get(e, self).to_rust(e))
  }

  pub fn values(&self, e: &mut java::Env) -> Vec<OptValue> {
    static VALUES: java::Field<Opt, ArrayList<OptValue>> = java::Field::new("values");
    VALUES.get(e, self).into_iter().collect()
  }
}
