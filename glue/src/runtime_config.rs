use jni::objects::JClass;
use jni::sys::jobject;
use toad::config::{self, BytesPerSecond, Config};
use toad::retry::{Attempts, Strategy};
use toad::time::Millis;
use toad_jni::java::{self, Class, Object};

use crate::retry_strategy::RetryStrategy;
use crate::uint;

pub struct RuntimeConfig(java::lang::Object);

java::object_newtype!(RuntimeConfig);
impl java::Class for RuntimeConfig {
  const PATH: &'static str = concat!(package!(dev.toad.Runtime), "$Config");
}

impl RuntimeConfig {
  pub fn port(&self, e: &mut java::Env) -> u16 {
    static RUNTIME_CONFIG_PORT: java::Field<RuntimeConfig, uint::u16> = java::Field::new("port");
    RUNTIME_CONFIG_PORT.get(e, self).to_rust(e)
  }

  pub fn concurrency(&self, e: &mut java::Env) -> u8 {
    static RUNTIME_CONFIG_CONCURRENCY: java::Field<RuntimeConfig, uint::u8> =
      java::Field::new("concurrency");
    RUNTIME_CONFIG_CONCURRENCY.get(e, self).to_rust(e)
  }

  pub fn msg(&self, e: &mut java::Env) -> Msg {
    static RUNTIME_CONFIG_MSG: java::Method<RuntimeConfig, fn() -> Msg> = java::Method::new("msg");
    RUNTIME_CONFIG_MSG.invoke(e, self)
  }

  pub fn new(e: &mut java::Env, c: Config, port: u16) -> Self {
    static CTOR: java::Constructor<RuntimeConfig, fn(uint::u16, uint::u8, Msg)> =
      java::Constructor::new();

    let con = Con::new(e,
                       c.msg.con.unacked_retry_strategy,
                       c.msg.con.acked_retry_strategy,
                       c.msg.con.max_attempts);
    let non = Non::new(e, c.msg.non.retry_strategy, c.msg.non.max_attempts);
    let msg = Msg::new(e,
                       c.msg.token_seed,
                       c.msg.probing_rate.0,
                       c.msg.multicast_response_leisure,
                       con,
                       non);

    let port = uint::u16::from_rust(e, port);
    let concurrency = uint::u8::from_rust(e, c.max_concurrent_requests);

    let jcfg = CTOR.invoke(e, port, concurrency, msg);
    jcfg
  }

  pub fn to_toad(&self, e: &mut java::Env) -> config::Config {
    let msg = self.msg(e);
    let con = msg.con(e);
    let non = msg.non(e);

    config::Config { max_concurrent_requests: self.concurrency(e) as u8,
                     msg: config::Msg { token_seed: msg.token_seed(e) as _,
                                        probing_rate: BytesPerSecond(msg.probing_rate(e) as _),
                                        multicast_response_leisure:
                                          msg.multicast_response_leisure(e),
                                        con: config::Con { unacked_retry_strategy:
                                                             con.unacked_retry_strategy(e),
                                                           acked_retry_strategy:
                                                             con.acked_retry_strategy(e),
                                                           max_attempts:
                                                             Attempts(con.max_attempts(e) as _) },
                                        non: config::Non { retry_strategy:
                                                             non.retry_strategy(e),
                                                           max_attempts:
                                                             Attempts(non.max_attempts(e) as _) } } }
  }
}

pub struct Msg(java::lang::Object);

java::object_newtype!(Msg);

impl java::Class for Msg {
  const PATH: &'static str = concat!(package!(dev.toad.Runtime), "$Config$Msg");
}

impl Msg {
  pub fn new(e: &mut java::Env,
             token_seed: u16,
             probe_rate: u16,
             multicast_response_leisure: Millis,
             con: Con,
             non: Non)
             -> Self {
    static CTOR: java::Constructor<Msg, fn(uint::u16, uint::u16, java::time::Duration, Con, Non)> =
      java::Constructor::new();
    let token_seed = uint::u16::from_rust(e, token_seed);
    let probe_rate = uint::u16::from_rust(e, probe_rate);
    let multicast_response_leisure =
      java::time::Duration::of_millis(e, multicast_response_leisure.0 as i64);

    CTOR.invoke(e,
                token_seed,
                probe_rate,
                multicast_response_leisure,
                con,
                non)
  }

  pub fn token_seed(&self, e: &mut java::Env) -> i32 {
    static TOKEN_SEED: java::Method<Msg, fn() -> i32> = java::Method::new("tokenSeed");
    TOKEN_SEED.invoke(e, self)
  }

  pub fn probing_rate(&self, e: &mut java::Env) -> i32 {
    static PROBING_RATE: java::Method<Msg, fn() -> i32> =
      java::Method::new("probingRateBytesPerSecond");
    PROBING_RATE.invoke(e, self)
  }

  pub fn multicast_response_leisure(&self, e: &mut java::Env) -> Millis {
    static MULTICAST_RESP_LEISURE: java::Method<Msg, fn() -> java::time::Duration> =
      java::Method::new("multicastResponseLeisure");
    let d = MULTICAST_RESP_LEISURE.invoke(e, self);

    Millis::new(d.to_millis(e) as u64)
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
  const PATH: &'static str = concat!(package!(dev.toad.Runtime), "$Config$Msg$Con");
}

impl Con {
  pub fn new(e: &mut java::Env,
             unacked: toad::retry::Strategy,
             acked: toad::retry::Strategy,
             max_attempts: Attempts)
             -> Self {
    static CTOR: java::Constructor<Con, fn(RetryStrategy, RetryStrategy, uint::u16)> =
      java::Constructor::new();
    let unacked = RetryStrategy::from_toad(e, unacked);
    let acked = RetryStrategy::from_toad(e, acked);
    let att = uint::u16::from_rust(e, max_attempts.0);
    CTOR.invoke(e, unacked, acked, att)
  }

  pub fn acked_retry_strategy(&self, e: &mut java::Env) -> Strategy {
    static ACKED_RETRY_STRATEGY: java::Method<Con, fn() -> RetryStrategy> =
      java::Method::new("ackedRetryStrategy");
    ACKED_RETRY_STRATEGY.invoke(e, self).to_toad(e)
  }

  pub fn unacked_retry_strategy(&self, e: &mut java::Env) -> Strategy {
    static UNACKED_RETRY_STRATEGY: java::Method<Con, fn() -> RetryStrategy> =
      java::Method::new("unackedRetryStrategy");
    UNACKED_RETRY_STRATEGY.invoke(e, self).to_toad(e)
  }

  pub fn max_attempts(&self, e: &mut java::Env) -> i32 {
    static MAX_ATTEMPTS: java::Method<Con, fn() -> i32> = java::Method::new("maxAttempts");
    MAX_ATTEMPTS.invoke(e, self)
  }
}

pub struct Non(java::lang::Object);

java::object_newtype!(Non);
impl java::Class for Non {
  const PATH: &'static str = concat!(package!(dev.toad.Runtime), "$Config$Msg$Non");
}

impl Non {
  pub fn new(e: &mut java::Env, strat: toad::retry::Strategy, max_attempts: Attempts) -> Self {
    static CTOR: java::Constructor<Non, fn(RetryStrategy, uint::u16)> = java::Constructor::new();
    let strat = RetryStrategy::from_toad(e, strat);
    let att = uint::u16::from_rust(e, max_attempts.0);
    CTOR.invoke(e, strat, att)
  }

  pub fn retry_strategy(&self, e: &mut java::Env) -> Strategy {
    static RETRY_STRATEGY: java::Method<Non, fn() -> RetryStrategy> =
      java::Method::new("retryStrategy");
    RETRY_STRATEGY.invoke(e, self).to_toad(e)
  }

  pub fn max_attempts(&self, e: &mut java::Env) -> i32 {
    static MAX_ATTEMPTS: java::Method<Non, fn() -> i32> = java::Method::new("maxAttempts");
    MAX_ATTEMPTS.invoke(e, self)
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Runtime_defaultConfigImpl<'local>(mut env: java::Env<'local>,
                                                                       _: JClass<'local>)
                                                                       -> jobject {
  RuntimeConfig::new(&mut env, Config::default(), 5683).yield_to_java(&mut env)
}
