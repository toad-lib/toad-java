pub mod ffi;
pub mod msg;

mod retry_strategy;

use jni::objects::{JClass, JObject, JThrowable};
use jni::sys::jobject;
pub use retry_strategy::RetryStrategy;
use toad::net::Addrd;
use toad::platform::{Platform, PlatformError};
use toad::retry::{Attempts, Strategy};
use toad::step::Step;
use toad::time::Millis;
use toad_jni::java::io::IOException;
use toad_jni::java::net::InetSocketAddress;
use toad_jni::java::nio::channels::{DatagramChannel, PeekableDatagramChannel};
use toad_jni::java::util::Optional;
use toad_jni::java::{self, Object, ResultYieldToJavaOrThrow};

use self::ffi::Ptr;
use crate::mem::{Shared, SharedMemoryRegion};
use crate::Runtime;

pub struct Toad(java::lang::Object);

impl Toad {
  pub fn new(e: &mut java::Env, cfg: Config, channel: PeekableDatagramChannel) -> Self {
    static CTOR: java::Constructor<Toad, fn(Config, PeekableDatagramChannel)> =
      java::Constructor::new();
    CTOR.invoke(e, cfg, channel)
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

  pub fn ptr(&self, e: &mut java::Env) -> Ptr {
    static PTR: java::Field<Toad, Ptr> = java::Field::new("ptr");
    PTR.get(e, self)
  }

  fn init_impl(e: &mut java::Env, cfg: Config, channel: PeekableDatagramChannel) -> i64 {
    let r = Runtime::new(&mut java::env(), cfg.log_level(e), cfg.to_toad(e), channel);
    unsafe { crate::mem::Shared::add_runtime(r).addr() as i64 }
  }

  fn notify_impl(&self,
                 e: &mut java::Env,
                 path: impl AsRef<str> + Clone)
                 -> Result<(), java::io::IOException> {
    self.ptr(e)
        .addr(e)
        .map_err(|err| IOException::new_caused_by(e, "", err))
        .map(|addr| unsafe { Shared::deref::<Runtime>(addr.inner(e)).as_ref().unwrap() })
        .and_then(|r| r.notify(path).map_err(PlatformError::step))
  }

  fn poll_req_impl(e: &mut java::Env, addr: i64) -> java::util::Optional<msg::ref_::Message> {
    let r = unsafe { Shared::deref::<Runtime>(addr).as_ref().unwrap() };
    match r.poll_req() {
      | Ok(req) => {
        let msg_ptr = unsafe { Shared::alloc_message(req.map(Into::into)) };
        let mr = msg::ref_::Message::new(e, msg_ptr.addr() as i64);
        java::util::Optional::<msg::ref_::Message>::of(e, mr)
      },
      | Err(nb::Error::WouldBlock) => java::util::Optional::<msg::ref_::Message>::empty(e),
      | Err(nb::Error::Other(err)) => {
        let err = err.downcast_ref(e).to_local(e);
        e.throw(jni::objects::JThrowable::from(err)).unwrap();
        java::util::Optional::<msg::ref_::Message>::empty(e)
      },
    }
  }

  fn poll_resp_impl(e: &mut java::Env,
                    addr: i64,
                    token: msg::Token,
                    sock: InetSocketAddress)
                    -> java::util::Optional<msg::ref_::Message> {
    match unsafe { Shared::deref::<Runtime>(addr).as_ref().unwrap() }.poll_resp(token.to_toad(e),
                                                                                sock.to_no_std(e))
    {
      | Ok(resp) => {
        let msg_ptr = unsafe { Shared::alloc_message(resp.map(Into::into)) };
        let mr = msg::ref_::Message::new(e, msg_ptr.addr() as i64);
        java::util::Optional::<msg::ref_::Message>::of(e, mr)
      },
      | Err(nb::Error::WouldBlock) => java::util::Optional::empty(e),
      | Err(nb::Error::Other(err)) => {
        let err = err.downcast_ref(e).to_local(e);
        e.throw(jni::objects::JThrowable::from(err)).unwrap();
        java::util::Optional::<msg::ref_::Message>::empty(e)
      },
    }
  }

  fn send_message_impl(e: &mut java::Env,
                       addr: i64,
                       msg: msg::owned::Message)
                       -> java::util::Optional<IdAndToken> {
    let r = unsafe { Shared::deref::<Runtime>(addr).as_ref().unwrap() };
    let sent = r.send_msg(Addrd(msg.to_toad(e), msg.addr(e).unwrap().to_no_std(e)));
    match sent {
      | Ok((id, token)) => {
        let out = IdAndToken::new(e, id, token);
        java::util::Optional::of(e, out)
      },
      | Err(nb::Error::WouldBlock) => java::util::Optional::empty(e),
      | Err(nb::Error::Other(err)) => {
        let err = err.downcast_ref(e).to_local(e);
        e.throw(jni::objects::JThrowable::from(err)).unwrap();
        java::util::Optional::empty(e)
      },
    }
  }
}

java::object_newtype!(Toad);

impl java::Class for Toad {
  const PATH: &'static str = package!(dev.toad.Toad);
}

pub struct IdAndToken(java::lang::Object);

java::object_newtype!(IdAndToken);
impl java::Class for IdAndToken {
  const PATH: &'static str = concat!(package!(dev.toad.Toad), "$IdAndToken");
}

impl IdAndToken {
  pub fn new(e: &mut java::Env, id: toad_msg::Id, token: toad_msg::Token) -> Self {
    static CTOR: java::Constructor<IdAndToken, fn(msg::Id, msg::Token)> = java::Constructor::new();
    let (id, token) = (msg::Id::from_toad(e, id), msg::Token::from_toad(e, token));
    CTOR.invoke(e, id, token)
  }
}

pub struct Config(java::lang::Object);

java::object_newtype!(Config);
impl java::Class for Config {
  const PATH: &'static str = concat!(package!(dev.toad.Toad), "$Config");
}

impl Config {
  pub fn concurrency(&self, e: &mut java::Env) -> u8 {
    static RUNTIME_CONFIG_CONCURRENCY: java::Field<Config, ffi::u8> =
      java::Field::new("concurrency");
    RUNTIME_CONFIG_CONCURRENCY.get(e, self).to_rust(e)
  }

  pub fn log_level(&self, e: &mut java::Env) -> java::util::logging::Level {
    static LOG_LEVEL: java::Method<Config, fn() -> java::util::logging::Level> =
      java::Method::new("logLevel");
    LOG_LEVEL.invoke(e, self)
  }

  pub fn msg(&self, e: &mut java::Env) -> Msg {
    static RUNTIME_CONFIG_MSG: java::Method<Config, fn() -> Msg> = java::Method::new("msg");
    RUNTIME_CONFIG_MSG.invoke(e, self)
  }

  pub fn new(e: &mut java::Env, c: toad::config::Config) -> Self {
    static CTOR: java::Constructor<Config, fn(Optional<java::util::logging::Level>, ffi::u8, Msg)> =
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

    let log_level: Optional<java::util::logging::Level> = Optional::empty(e);
    let jcfg = CTOR.invoke(e, log_level, concurrency, msg);
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
  Config::new(&mut env, toad::config::Config::default()).yield_to_java(&mut env)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_init<'local>(mut e: java::Env<'local>,
                                                       _: JClass<'local>,
                                                       channel: JObject<'local>,
                                                       cfg: JObject<'local>)
                                                       -> i64 {
  let e = &mut e;
  let cfg = java::lang::Object::from_local(e, cfg).upcast_to::<Config>(e);
  let channel = java::lang::Object::from_local(e, channel).upcast_to::<DatagramChannel>(e);

  Toad::init_impl(e, cfg, channel.peekable())
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_sendMessage<'local>(mut e: java::Env<'local>,
                                                              _: JClass<'local>,
                                                              addr: i64,
                                                              msg: JObject<'local>)
                                                              -> jobject {
  let e = &mut e;
  let msg: msg::owned::Message = java::lang::Object::from_local(e, msg).upcast_to(e);
  Toad::send_message_impl(e, addr, msg).yield_to_java(e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_pollReq<'local>(mut e: java::Env<'local>,
                                                          _: JClass<'local>,
                                                          addr: i64)
                                                          -> jobject {
  let e = &mut e;
  Toad::poll_req_impl(e, addr).yield_to_java(e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_pollResp<'local>(mut e: java::Env<'local>,
                                                           _: JClass<'local>,
                                                           addr: i64,
                                                           token: JObject<'local>,
                                                           sock: JObject<'local>)
                                                           -> jobject {
  let e = &mut e;
  let token = java::lang::Object::from_local(e, token).upcast_to::<msg::Token>(e);
  let sock = java::lang::Object::from_local(e, sock).upcast_to::<InetSocketAddress>(e);
  Toad::poll_resp_impl(e, addr, token, sock).yield_to_java(e)
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_notify<'local>(mut e: java::Env<'local>,
                                                         toad: JObject<'local>,
                                                         path: JObject<'local>)
                                                         -> () {
  let e = &mut e;
  let toad = java::lang::Object::from_local(e, toad).upcast_to::<Toad>(e);
  let path = java::lang::Object::from_local(e, path).upcast_to::<String>(e);

  if let Err(err) = toad.notify_impl(e, path) {
    let err = JThrowable::from(err.downcast(e).to_local(e));
    e.throw(err).unwrap()
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_Toad_teardown<'local>(_: java::Env<'local>,
                                                           _: JClass<'local>)
                                                           -> () {
  unsafe {
    crate::mem::Shared::dealloc();
  }
}
