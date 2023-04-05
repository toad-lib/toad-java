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

  pub const EXPONENTIAL: &'static str = package!(dev.toad.RetryStrategyExponential);
  pub const EXPONENTIAL_CTOR: Sig = Sig::new().arg(Sig::class(java::time::Duration::PATH))
                                              .arg(Sig::class(java::time::Duration::PATH))
                                              .returning(Sig::VOID);

  pub const DELAY: &'static str = package!(dev.toad.RetryStrategyDelay);
  pub const DELAY_CTOR: Sig = Sig::new().arg(Sig::class(java::time::Duration::PATH))
                                        .arg(Sig::class(java::time::Duration::PATH))
                                        .returning(Sig::VOID);

  pub fn exp<'a>(&self, e: &mut JNIEnv<'a>) -> Self {
    let o = e.new_object(Self::PATH, Sig::new().returning(Sig::VOID), &[])
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    Self(g)
  }

  pub fn millis_field<'a>(&self, e: &mut JNIEnv<'a>, key: &str) -> Millis {
    let o = e.get_field(&self.0, key, Sig::class(java::time::Duration::PATH))
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

  pub fn from_toad<'a>(e: &mut JNIEnv<'a>, s: Strategy) -> Self {
    let g = match s {
      | Strategy::Delay { min, max } => {
        let (min, max) = (java::time::Duration::of_millis(e, min.0 as i64),
                          java::time::Duration::of_millis(e, max.0 as i64));
        let (min, max) = (min.to_java(), max.to_java());
        let o = e.new_object(Self::DELAY,
                             Self::DELAY_CTOR,
                             &[min.as_obj().into(), max.as_obj().into()])
                 .unwrap();
        e.new_global_ref(o).unwrap()
      },
      | Strategy::Exponential { init_min, init_max } => {
        let (init_min, init_max) = (java::time::Duration::of_millis(e, init_min.0 as i64),
                                    java::time::Duration::of_millis(e, init_max.0 as i64));
        let (init_min, init_max) = (init_min.to_java(), init_max.to_java());
        let o = e.new_object(Self::EXPONENTIAL,
                             Self::EXPONENTIAL_CTOR,
                             &[init_min.as_obj().into(), init_max.as_obj().into()])
                 .unwrap();
        e.new_global_ref(o).unwrap()
      },
    };

    Self(g)
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
