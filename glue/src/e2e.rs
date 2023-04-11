use no_std_net::SocketAddr;
use toad::config::Config;
use toad::net::Addrd;
use toad::platform::Platform;
use toad_jni::java::{self, Object, Signature};
use toad_msg::alloc::Message;
use toad_msg::{Code, Id, Token, Type};

use crate::{dev, Runtime};

#[non_exhaustive]
struct State {
  pub runtime: dev::toad::Toad,
  pub env: java::Env<'static>,
  pub client: crate::Runtime,
  pub srv_addr: SocketAddr,
}

fn init() -> State {
  let mut _env = crate::test::init();
  let env = &mut _env;

  let cfg =
    dev::toad::Config::new(env,
                           Config::default(),
                           std::net::SocketAddr::new(std::net::Ipv4Addr::UNSPECIFIED.into(), 5683));
  let runtime = dev::toad::Toad::new(env, cfg);
  let client = Runtime::try_new("0.0.0.0:5684", Default::default()).unwrap();

  State { runtime,
          env: _env,
          client,
          srv_addr: "0.0.0.0:5683".parse().unwrap() }
}

fn runtime_poll_req(State { runtime,
                            env,
                            client,
                            srv_addr,
                            .. }: &mut State) {
  assert!(runtime.poll_req(env).is_none());

  let request = Message::new(Type::Con, Code::GET, Id(0), Token(Default::default()));
  client.send_msg(Addrd(request, *srv_addr)).unwrap();

  assert!(runtime.poll_req(env).is_some());
}

#[test]
fn e2e_test_suite() {
  let mut state = init();
  runtime_poll_req(&mut state);
}
