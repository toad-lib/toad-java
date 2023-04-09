use toad::config::{self, BytesPerSecond};
use toad::retry::{Attempts, Strategy};
use toad::time::Millis;
use toad_jni::java;

use crate::retry_strategy::RetryStrategy;
use crate::uint;

pub struct RuntimeConfig(java::lang::Object);

java::object_newtype!(RuntimeConfig);
impl java::Class for RuntimeConfig {
  const PATH: &'static str = package!(dev.toad.RuntimeOptions);
}

impl RuntimeConfig {
  pub fn new(e: &mut java::Env) -> Self {
    static CTOR: java::Constructor<RuntimeConfig, fn()> = java::Constructor::new();
    CTOR.invoke(e)
  }

  pub fn net(&self, e: &mut java::Env) -> Net {
    static NET: java::Method<RuntimeConfig, fn() -> Net> = java::Method::new("net");
    NET.invoke(e, self)
  }

  pub fn to_toad(&self, e: &mut java::Env) -> config::Config {
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

pub struct Net(java::lang::Object);
java::object_newtype!(Net);
impl java::Class for Net {
  const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Net");
}

static NET_PORT: java::Field<Net, uint::u16> = java::Field::new("port");

impl Net {
  pub fn port(&self, e: &mut java::Env) -> u16 {
    NET_PORT.get(e, self).to_rust(e)
  }

  pub fn set_port(&self, e: &mut java::Env, new: u16) {
    let new = uint::u16::from_rust(e, new);
    NET_PORT.set(e, self, new)
  }

  pub fn concurrency(&self, e: &mut java::Env) -> u8 {
    static CONCURRENCY: java::Field<Net, uint::u8> = java::Field::new("concurrency");
    CONCURRENCY.get(e, self).to_rust(e)
  }

  pub fn msg(&self, e: &mut java::Env) -> Msg {
    static MSG: java::Method<Net, fn() -> Msg> = java::Method::new("msg");
    MSG.invoke(e, self)
  }
}

pub struct Msg(java::lang::Object);

java::object_newtype!(Msg);

impl java::Class for Msg {
  const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Msg");
}

impl Msg {
  pub fn token_seed(&self, e: &mut java::Env) -> Option<i32> {
    static TOKEN_SEED: java::Method<Msg, fn() -> java::util::Optional<i32>> =
      java::Method::new("tokenSeed");
    TOKEN_SEED.invoke(e, self).to_option(e)
  }

  pub fn probing_rate(&self, e: &mut java::Env) -> Option<i32> {
    static PROBING_RATE: java::Method<Msg, fn() -> java::util::Optional<i32>> =
      java::Method::new("probingRateBytesPerSecond");
    PROBING_RATE.invoke(e, self).to_option(e)
  }

  pub fn multicast_response_leisure(&self, e: &mut java::Env) -> Option<Millis> {
    static MULTICAST_RESP_LEISURE: java::Method<Msg,
                                                  fn()
                                                     -> java::util::Optional<java::time::Duration>> =
      java::Method::new("multicastResponseLeisure");
    MULTICAST_RESP_LEISURE.invoke(e, self)
                          .to_option(e)
                          .map(|d| Millis::new(d.to_millis(e) as u64))
  }

  pub fn con(&self, e: &mut java::Env) -> Con {
    static CON: java::Method<Msg, fn() -> Con> = java::Method::new("con");
    CON.invoke(e, self)
  }

  pub fn non(&self, e: &mut java::Env) -> Non {
    static NON: java::Method<Msg, fn() -> Non> = java::Method::new("non");
    NON.invoke(e, self)
  }
}

pub struct Con(java::lang::Object);

java::object_newtype!(Con);
impl java::Class for Con {
  const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Msg$Con");
}

impl Con {
  pub fn acked_retry_strategy(&self, e: &mut java::Env) -> Option<Strategy> {
    static ACKED_RETRY_STRATEGY: java::Method<Con, fn() -> java::util::Optional<RetryStrategy>> =
      java::Method::new("ackedRetryStrategy");
    ACKED_RETRY_STRATEGY.invoke(e, self)
                        .to_option(e)
                        .map(|s| s.to_toad(e))
  }

  pub fn unacked_retry_strategy(&self, e: &mut java::Env) -> Option<Strategy> {
    static UNACKED_RETRY_STRATEGY: java::Method<Con, fn() -> java::util::Optional<RetryStrategy>> =
      java::Method::new("unackedRetryStrategy");
    UNACKED_RETRY_STRATEGY.invoke(e, self)
                          .to_option(e)
                          .map(|s| s.to_toad(e))
  }

  pub fn max_attempts(&self, e: &mut java::Env) -> Option<i32> {
    static MAX_ATTEMPTS: java::Method<Con, fn() -> java::util::Optional<i32>> =
      java::Method::new("maxAttempts");
    MAX_ATTEMPTS.invoke(e, self).to_option(e)
  }
}

pub struct Non(java::lang::Object);

java::object_newtype!(Non);
impl java::Class for Non {
  const PATH: &'static str = concat!(package!(dev.toad.RuntimeOptions), "$Msg$Non");
}

impl Non {
  pub fn retry_strategy(&self, e: &mut java::Env) -> Option<Strategy> {
    static RETRY_STRATEGY: java::Method<Non, fn() -> java::util::Optional<RetryStrategy>> =
      java::Method::new("retryStrategy");
    RETRY_STRATEGY.invoke(e, self)
                  .to_option(e)
                  .map(|s| s.to_toad(e))
  }

  pub fn max_attempts(&self, e: &mut java::Env) -> Option<i32> {
    static MAX_ATTEMPTS: java::Method<Non, fn() -> java::util::Optional<i32>> =
      java::Method::new("maxAttempts");
    MAX_ATTEMPTS.invoke(e, self).to_option(e)
  }
}
