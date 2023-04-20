use std::time::{Duration, Instant};

use no_std_net::SocketAddr;
use toad::config::Config;
use toad::net::Addrd;
use toad::platform::Platform;
use toad_jni::java::nio::channels::PeekableDatagramChannel;
use toad_jni::java::{self};
use toad_msg::alloc::Message;
use toad_msg::{Code, Id, Token, Type};

use crate::dev;

type RustRuntime =
  toad::std::Platform<toad::std::dtls::N, toad::step::runtime::std::Runtime<toad::std::dtls::N>>;

#[non_exhaustive]
struct State {
  pub runtime: dev::toad::Toad,
  pub env: java::Env<'static>,
  pub client: RustRuntime,
  pub srv_addr: SocketAddr,
}

fn init() -> State {
  let mut _env = crate::test::init();
  let env = &mut _env;

  let cfg = dev::toad::Config::new(env, Config::default());
  let sock = <PeekableDatagramChannel as toad::net::Socket>::bind(no_std_net::SocketAddr::new(no_std_net::Ipv4Addr::LOCALHOST.into(), 5683)).unwrap();
  let runtime = dev::toad::Toad::new(env, cfg, sock);
  let client = RustRuntime::try_new("127.0.0.1:5684", Default::default()).unwrap();

  State { runtime,
          env: _env,
          client,
          srv_addr: "127.0.0.1:5683".parse().unwrap() }
}

fn runtime_poll_req(State { runtime,
                            env,
                            client,
                            srv_addr,
                            .. }: &mut State) {
  assert!(runtime.poll_req(env).is_none());

  let request = Message::new(Type::Con, Code::GET, Id(0), Token(Default::default()));
  client.send_msg(Addrd(request, *srv_addr)).unwrap();

  let start = Instant::now();
  loop {
    if Instant::now() - start > Duration::from_millis(10000) {
      panic!("timed out waiting for DatagramChannel to receive message");
    } else if runtime.poll_req(env).is_some() {
      break;
    }
  }
}

#[test]
fn e2e_test_suite() {
  let mut state = init();
  runtime_poll_req(&mut state);
}
