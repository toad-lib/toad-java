use jni::objects::{GlobalRef, JObject};
use jni::sys::{jint, jshort};
use jni::JNIEnv;
use toad::config::{self, BytesPerSecond};
use toad::retry::{Attempts, Strategy};
use toad::time::Millis;
use toad_jni::cls::java;
use toad_jni::convert::{Object, Primitive};
use toad_jni::Sig;

use crate::retry_strategy::RetryStrategy;

pub struct RuntimeConfig<'a>(JObject<'a>);

impl<'a> RuntimeConfig<'a> {
  pub const PATH: &'static str = package!(dev.toad.RuntimeOptions);

  pub const CTOR: Sig = Sig::new().returning(Sig::VOID);

  pub fn new(e: &mut JNIEnv<'a>) -> Self {
    let o = e.new_object(Self::PATH, Self::CTOR, &[]).unwrap();
    Self(o)
  }

  pub fn net(&self, e: &mut JNIEnv<'a>) -> Net<'a> {
    let o = e.call_method(&self.0,
                          "net",
                          Sig::new().returning(Sig::class(Net::PATH)),
                          &[])
             .unwrap()
             .l()
             .unwrap();
    Net(o)
  }

  pub fn to_toad(&self, e: &mut JNIEnv<'a>) -> config::Config {
    let def = config::Config::default();

    let net = self.net(e);
    let msg = net.msg(e);
    let con = msg.con(e);
    let non = msg.non(e);

    config::Config { max_concurrent_requests: net.concurrency(e) as u8,
                     msg: config::Msg { token_seed: msg.token_seed(e)
                                                       .map(|i| i as u16)
                                                       .unwrap_or(def.msg.token_seed),
                                        probing_rate: msg.probing_rate(e)
                                                         .map(|i| BytesPerSecond(i as u16))
                                                         .unwrap_or(def.msg.probing_rate),
                                        multicast_response_leisure:
                                          msg.multicast_response_leisure(e)
                                             .unwrap_or(def.msg.multicast_response_leisure),
                                        con:
                                          config::Con { unacked_retry_strategy:
                                                          con.unacked_retry_strategy(e)
                                                             .unwrap_or(def.msg
                                                                           .con
                                                                           .unacked_retry_strategy),
                                                        acked_retry_strategy:
                                                          con.acked_retry_strategy(e)
                                                             .unwrap_or(def.msg
                                                                           .con
                                                                           .acked_retry_strategy),
                                                        max_attempts:
                                                          con.max_attempts(e)
                                                             .map(|i| Attempts(i as u16))
                                                             .unwrap_or(def.msg.con.max_attempts) },
                                        non: config::Non { retry_strategy:
                                                             non.retry_strategy(e)
                                                                .unwrap_or(def.msg
                                                                              .non
                                                                              .retry_strategy),
                                                           max_attempts:
                                                             non.max_attempts(e)
                                                                .map(|i| Attempts(i as u16))
                                                                .unwrap_or(def.msg.non.max_attempts) } } }
  }
}

pub struct Net<'a>(JObject<'a>);
impl<'a> Net<'a> {
  pub const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Net");

  pub fn port(&self, e: &mut JNIEnv<'a>) -> jshort {
    e.call_method(&self.0, "port", Sig::new().returning(Sig::SHORT), &[])
     .unwrap()
     .s()
     .unwrap()
  }

  pub fn concurrency(&self, e: &mut JNIEnv<'a>) -> jshort {
    e.call_method(&self.0,
                  "concurrency",
                  Sig::new().returning(Sig::SHORT),
                  &[])
     .unwrap()
     .s()
     .unwrap()
  }

  pub fn msg(&self, e: &mut JNIEnv<'a>) -> Msg<'a> {
    let o = e.call_method(&self.0,
                          "msg",
                          Sig::new().returning(Sig::class(Msg::PATH)),
                          &[])
             .unwrap()
             .l()
             .unwrap();
    Msg(o)
  }
}

pub struct Msg<'a>(JObject<'a>);
impl<'a> Msg<'a> {
  pub const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Msg");

  pub const TOKEN_SEED: Sig = Sig::new().returning(Sig::class(java::util::Optional::<jint>::PATH));
  pub const PROBING_RATE: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<jint>::PATH));
  pub const MULTICAST_RESP_LEISURE: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<GlobalRef>::PATH));
  pub const CON: Sig = Sig::new().returning(Sig::class(Con::PATH));
  pub const NON: Sig = Sig::new().returning(Sig::class(Non::PATH));

  pub fn token_seed(&self, e: &mut JNIEnv<'a>) -> Option<jint> {
    let o = e.call_method(&self.0, "tokenSeed", Self::TOKEN_SEED, &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<jint>::from_java(g).to_option(e)
  }

  pub fn probing_rate(&self, e: &mut JNIEnv<'a>) -> Option<jint> {
    let o = e.call_method(&self.0,
                          "probingRateBytesPerSecond",
                          Self::PROBING_RATE,
                          &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<jint>::from_java(g).to_option(e)
  }

  pub fn multicast_response_leisure(&self, e: &mut JNIEnv<'a>) -> Option<Millis> {
    let o = e.call_method(&self.0,
                          "multicastResponseLeisure",
                          Self::MULTICAST_RESP_LEISURE,
                          &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<GlobalRef>::from_java(g).to_option(e)
                                                   .map(java::time::Duration::from_java)
                                                   .map(|d| Millis::new(d.to_millis(e) as u64))
  }

  pub fn con(&self, e: &mut JNIEnv<'a>) -> Con<'a> {
    let o = e.call_method(&self.0, "con", Self::CON, &[])
             .unwrap()
             .l()
             .unwrap();
    Con(o)
  }

  pub fn non(&self, e: &mut JNIEnv<'a>) -> Non<'a> {
    let o = e.call_method(&self.0, "non", Self::NON, &[])
             .unwrap()
             .l()
             .unwrap();
    Non(o)
  }
}

pub struct Con<'a>(JObject<'a>);
impl<'a> Con<'a> {
  pub const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Msg$Con");

  pub const ACKED_RETRY_STRATEGY: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<GlobalRef>::PATH));
  pub const UNACKED_RETRY_STRATEGY: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<GlobalRef>::PATH));
  pub const MAX_ATTEMPTS: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<jint>::PATH));

  pub fn acked_retry_strategy(&self, e: &mut JNIEnv<'a>) -> Option<Strategy> {
    let o = e.call_method(&self.0,
                          "ackedRetryStrategy",
                          Self::ACKED_RETRY_STRATEGY,
                          &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<GlobalRef>::from_java(g).to_option(e)
                                                   .map(RetryStrategy::from_java)
                                                   .map(|j| j.to_toad(e))
  }

  pub fn unacked_retry_strategy(&self, e: &mut JNIEnv<'a>) -> Option<Strategy> {
    let o = e.call_method(&self.0,
                          "unackedRetryStrategy",
                          Self::UNACKED_RETRY_STRATEGY,
                          &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<GlobalRef>::from_java(g).to_option(e)
                                                   .map(RetryStrategy::from_java)
                                                   .map(|j| j.to_toad(e))
  }

  pub fn max_attempts(&self, e: &mut JNIEnv<'a>) -> Option<jint> {
    let o = e.call_method(&self.0, "maxAttempts", Self::MAX_ATTEMPTS, &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<GlobalRef>::from_java(g).to_option(e)
                                                   .map(java::lang::Integer::from_java)
                                                   .map(Primitive::dewrap)
  }
}

pub struct Non<'a>(JObject<'a>);
impl<'a> Non<'a> {
  pub const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Msg$Non");

  pub const RETRY_STRATEGY: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<GlobalRef>::PATH));
  pub const MAX_ATTEMPTS: Sig =
    Sig::new().returning(Sig::class(java::util::Optional::<jint>::PATH));

  pub fn retry_strategy(&self, e: &mut JNIEnv<'a>) -> Option<Strategy> {
    let o = e.call_method(&self.0, "retryStrategy", Self::RETRY_STRATEGY, &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<GlobalRef>::from_java(g).to_option(e)
                                                   .map(RetryStrategy::from_java)
                                                   .map(|j| j.to_toad(e))
  }

  pub fn max_attempts(&self, e: &mut JNIEnv<'a>) -> Option<jint> {
    let o = e.call_method(&self.0, "maxAttempts", Self::MAX_ATTEMPTS, &[])
             .unwrap()
             .l()
             .unwrap();
    let g = e.new_global_ref(o).unwrap();
    java::util::Optional::<GlobalRef>::from_java(g).to_option(e)
                                                   .map(java::lang::Integer::from_java)
                                                   .map(Primitive::dewrap)
  }
}
