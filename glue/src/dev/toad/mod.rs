pub mod ffi;
pub mod msg;

mod retry_strategy;
use std::net::{Ipv4Addr, SocketAddr};

use jni::objects::{JClass, JObject};
use jni::sys::jobject;
pub use retry_strategy::RetryStrategy;
use toad::platform::Platform;
use toad::retry::{Attempts, Strategy};
use toad::time::Millis;
use toad_jni::java::{self, Object};

use crate::mem::{Shared, SharedMemoryRegion};
use crate::Runtime;

pub struct Toad(java::lang::Object);

impl Toad {
  pub fn new(e: &mut java::Env, cfg: Config) -> Self {
    static CTOR: java::Constructor<Toad, fn(Config)> = java::Constructor::new();
    CTOR.invoke(e, cfg)
  }

  pub fn poll_req(&self, e: &mut java::Env) -> Option<msg::ref_::Message> {
    static POLL_REQ: java::Method<Toad, fn() -> java::util::Optional<msg::ref_::Message>> =
      java::Method::new("pollReq");
    POLL_REQ.invoke(e, self).to_option(e)
  }

  pub fn config(&self, e: &mut java::Env) -> Config {
    static CONFIG: java::Method<Toad, fn() -> Config> = java::Method::new("config");
    CONFIG.invoke(e, self)
  }

  fn init_impl(e: &mut java::Env, cfg: Config) -> i64 {
    let r = || Runtime::try_new(cfg.addr(e), cfg.to_toad(e)).unwrap();
    unsafe { crate::mem::Shared::init(r).addr() as i64 }
  }

  fn poll_req_impl(e: &mut java::Env, addr: i64) -> java::util::Optional<msg::ref_::Message> {
    match unsafe {
            Shared::deref::<Runtime>(/* TODO */ 0, addr).as_ref()
                                                        .unwrap()
          }.poll_req()
    {
      | Ok(req) => {
        let mr = msg::ref_::Message::new(e, req.unwrap().into());
        java::util::Optional::<msg::ref_::Message>::of(e, mr)
      },
      | Err(nb::Error::WouldBlock) => java::util::Optional::<msg::ref_::Message>::empty(e),
      | Err(nb::Error::Other(err)) => {
        e.throw(format!("{:?}", err)).unwrap();
        java::util::Optional::<msg::ref_::Message>::empty(e)
      },
    }
  }
}

java::object_newtype!(Toad);

impl java::Class for Toad {
  const PATH: &'static str = package!(dev.toad.Toad);
}

pub struct Config(java::lang::Object);

java::object_newtype!(Config);
impl java::Class for Config {
  const PATH: &'static str = concat!(package!(dev.toad.Toad), "$Config");
}

impl Config {
  pub fn addr(&self, e: &mut java::Env) -> SocketAddr {
    static ADDRESS: java::Field<Config, java::net::InetSocketAddress> = java::Field::new("addr");
    ADDRESS.get(e, self).to_std(e)
  }

  pub fn concurrency(&self, e: &mut java::Env) -> u8 {
    static RUNTIME_CONFIG_CONCURRENCY: java::Field<Config, ffi::u8> =
      java::Field::new("concurrency");
    RUNTIME_CONFIG_CONCURRENCY.get(e, self).to_rust(e)
  }

  pub fn msg(&self, e: &mut java::Env) -> Msg {
    static RUNTIME_CONFIG_MSG: java::Method<Config, fn() -> Msg> = java::Method::new("msg");
    RUNTIME_CONFIG_MSG.invoke(e, self)
  }

  pub fn new(e: &mut java::Env, c: toad::config::Config, addr: SocketAddr) -> Self {
    static CTOR: java::Constructor<Config, fn(java::net::InetSocketAddress, ffi::u8, Msg)> =
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

    let concurrency = ffi::u8::from_rust(e, c.max_concurrent_requests);

    let address = java::net::InetSocketAddress::from_std(e, addr);

    let jcfg = CTOR.invoke(e, address, concurrency, msg);
    jcfg
  }

  pub fn to_toad(&self, e: &mut java::Env) -> toad::config::Config {
    let msg = self.msg(e);
    let con = msg.con(e);
    let non = msg.non(e);

    toad::config::Config { max_concurrent_requests: self.concurrency(e) as u8,
                     msg: toad::config::Msg { token_seed: msg.token_seed(e) as _,
                                        probing_rate: toad::config::BytesPerSecond(msg.probing_rate(e) as _),
                                        multicast_response_leisure:
                                          msg.multicast_response_leisure(e),
                                        con: toad::config::Con { unacked_retry_strategy:
                                                             con.unacked_retry_strategy(e),
                                                           acked_retry_strategy:
                                                             con.acked_retry_strategy(e),
                                                           max_attempts:
                                                             Attempts(con.max_attempts(e) as _) },
                                        non: toad::config::Non { retry_strategy:
                                                             non.retry_strategy(e),
                                                           max_attempts:
                                                             Attempts(non.max_attempts(e) as _) } } }
  }
}

pub struct Msg(java::lang::Object);

java::object_newtype!(Msg);

impl java::Class for Msg {
  const PATH: &'static str = concat!(package!(dev.toad.Toad), "$Config$Msg");
}

impl Msg {
  pub fn new(e: &mut java::Env,
             token_seed: u16,
             probe_rate: u16,
             multicast_response_leisure: Millis,
             con: Con,
             non: Non)
             -> Self {
    static CTOR: java::Constructor<Msg, fn(ffi::u16, ffi::u16, java::time::Duration, Con, Non)> =
      java::Constructor::new();
    let token_seed = ffi::u16::from_rust(e, token_seed);
    let probe_rate = ffi::u16::from_rust(e, probe_rate);
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
  const PATH: &'static str = concat!(package!(dev.toad.Toad), "$Config$Msg$Con");
}

impl Con {
  pub fn new(e: &mut java::Env,
             unacked: Strategy,
             acked: Strategy,
             max_attempts: Attempts)
             -> Self {
    static CTOR: java::Constructor<Con, fn(RetryStrategy, RetryStrategy, ffi::u16)> =
      java::Constructor::new();
    let unacked = RetryStrategy::from_toad(e, unacked);
    let acked = RetryStrategy::from_toad(e, acked);
    let att = ffi::u16::from_rust(e, max_attempts.0);
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
  const PATH: &'static str = concat!(package!(dev.toad.Toad), "$Config$Msg$Non");
}

impl Non {
  pub fn new(e: &mut java::Env, strat: Strategy, max_attempts: Attempts) -> Self {
    static CTOR: java::Constructor<Non, fn(RetryStrategy, ffi::u16)> = java::Constructor::new();
    let strat = RetryStrategy::from_toad(e, strat);
    let att = ffi::u16::from_rust(e, max_attempts.0);
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
pub extern "system" fn Java_dev_toad_Toad_defaultConfigImpl<'local>(mut env: java::Env<'local>,
                                                                    _: JClass<'local>)
                                                                    -> jobject {
  Config::new(&mut env,
              toad::config::Config::default(),
              SocketAddr::new(Ipv4Addr::UNSPECIFIED.into(), 5683)).yield_to_java(&mut env)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_init<'local>(mut e: java::Env<'local>,
                                                       _: JClass<'local>,
                                                       cfg: JObject<'local>)
                                                       -> i64 {
  let e = &mut e;
  let cfg = java::lang::Object::from_local(e, cfg).upcast_to::<Config>(e);

  Toad::init_impl(e, cfg)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_pollReq<'local>(mut e: java::Env<'local>,
                                                          _: JClass<'local>,
                                                          addr: i64)
                                                          -> jobject {
  let e = &mut e;
  Toad::poll_req_impl(e, addr).yield_to_java(e)
}
