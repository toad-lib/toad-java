use jni::objects::{GlobalRef, JObject};
use jni::JNIEnv;
use toad::retry::Strategy;
use toad::time::Millis;
use toad_jni::cls::java;
use toad_jni::convert::Object;
use toad_jni::Sig;

pub struct RetryStrategy(GlobalRef);

impl RetryStrategy {
  pub const PATH: &'static str = package!(dev.toad.RetryStrategy);
  pub const EXPONENTIAL: &'static str = package!(dev.toad.RetryStrategy.Exponential);
  pub const LINEAR: &'static str = package!(dev.toad.RetryStrategy.Linear);

  pub fn exp<'a>(&self, e: &mut JNIEnv<'a>) -> Self {
    let o = e.new_object(Self::PATH, Sig::new().returning(Sig::VOID), &[])
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    Self(g)
  }

  pub fn millis_field<'a>(&self, e: &mut JNIEnv<'a>, key: &str) -> Millis {
    let o = e.get_field(&self.0, "initMax", Sig::class(java::time::Duration::PATH))
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    let d = java::time::Duration::from_java(g);
    Millis::new(d.to_millis(e) as u64)
  }

  pub fn to_toad<'a>(self, e: &mut JNIEnv<'a>) -> Strategy {
    if e.is_instance_of(&self.0, Self::EXPONENTIAL).unwrap() {
      Strategy::Exponential { init_min: self.millis_field(e, "initMin"),
                              init_max: self.millis_field(e, "initMax") }
    } else {
      Strategy::Delay { min: self.millis_field(e, "min"),
                        max: self.millis_field(e, "max") }
    }
  }
}

impl Object for RetryStrategy {
  fn from_java(jobj: GlobalRef) -> Self {
    Self(jobj)
  }

  fn to_java(self) -> GlobalRef {
    self.0
  }
}
