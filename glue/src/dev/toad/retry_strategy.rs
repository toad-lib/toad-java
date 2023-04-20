use toad::retry::Strategy;
use toad::time::Millis;
use toad_jni::java;

use crate::dev::toad::ffi;

pub struct RetryStrategyExp(java::lang::Object);

java::object_newtype!(RetryStrategyExp);
impl java::Class for RetryStrategyExp {
  const PATH: &'static str = package!(dev.toad.RetryStrategyExponential);
}

impl RetryStrategyExp {
  pub fn new(e: &mut java::Env, init_min: Millis, init_max: Millis) -> Self {
    let (init_min, init_max) = (java::time::Duration::of_millis(e, init_min.0 as i64),
                                java::time::Duration::of_millis(e, init_max.0 as i64));
    static CTOR: java::Constructor<RetryStrategyExp,
                                     fn(java::time::Duration, java::time::Duration)> =
      java::Constructor::new();
    CTOR.invoke(e, init_min, init_max)
  }

  pub fn as_super(self) -> RetryStrategy {
    RetryStrategy(self.0)
  }

  pub fn init_min(&self, e: &mut java::Env) -> Millis {
    static INIT_MIN: java::Field<RetryStrategyExp, ffi::u64> = java::Field::new("initMin");
    Millis::new(INIT_MIN.get(e, self).to_rust(e))
  }

  pub fn init_max(&self, e: &mut java::Env) -> Millis {
    static INIT_MAX: java::Field<RetryStrategyExp, ffi::u64> = java::Field::new("initMax");
    Millis::new(INIT_MAX.get(e, self).to_rust(e))
  }
}

pub struct RetryStrategyDelay(java::lang::Object);

java::object_newtype!(RetryStrategyDelay);
impl java::Class for RetryStrategyDelay {
  const PATH: &'static str = package!(dev.toad.RetryStrategyDelay);
}

impl RetryStrategyDelay {
  pub fn new(e: &mut java::Env, min: Millis, max: Millis) -> Self {
    let (min, max) = (java::time::Duration::of_millis(e, min.0 as i64),
                      java::time::Duration::of_millis(e, max.0 as i64));
    static CTOR: java::Constructor<RetryStrategyDelay,
                                     fn(java::time::Duration, java::time::Duration)> =
      java::Constructor::new();
    CTOR.invoke(e, min, max)
  }

  pub fn as_super(self) -> RetryStrategy {
    RetryStrategy(self.0)
  }

  pub fn min(&self, e: &mut java::Env) -> Millis {
    static MIN: java::Field<RetryStrategyDelay, ffi::u64> = java::Field::new("min");
    Millis::new(MIN.get(e, self).to_rust(e))
  }

  pub fn max(&self, e: &mut java::Env) -> Millis {
    static MAX: java::Field<RetryStrategyDelay, ffi::u64> = java::Field::new("max");
    Millis::new(MAX.get(e, self).to_rust(e))
  }
}

pub struct RetryStrategy(java::lang::Object);

java::object_newtype!(RetryStrategy);
impl java::Class for RetryStrategy {
  const PATH: &'static str = package!(dev.toad.RetryStrategy);
}

impl RetryStrategy {
  pub fn to_toad(self, e: &mut java::Env) -> Strategy {
    if self.0.is_instance_of::<RetryStrategyExp>(e) {
      let me = RetryStrategyExp(self.0);
      Strategy::Exponential { init_min: me.init_min(e),
                              init_max: me.init_max(e) }
    } else if self.0.is_instance_of::<RetryStrategyDelay>(e) {
      let me = RetryStrategyDelay(self.0);
      Strategy::Delay { min: me.min(e),
                        max: me.max(e) }
    } else {
      let cls = e.get_object_class(self.0).unwrap();
      panic!("unknown inheritor of RetryStrategy: {}",
             java::lang::Object::from_local(e, cls).to_string(e));
    }
  }

  pub fn from_toad(e: &mut java::Env, s: Strategy) -> Self {
    match s {
      | Strategy::Delay { min, max } => RetryStrategyDelay::new(e, min, max).as_super(),
      | Strategy::Exponential { init_min, init_max } => {
        RetryStrategyExp::new(e, init_min, init_max).as_super()
      },
    }
  }
}
