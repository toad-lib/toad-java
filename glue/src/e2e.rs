use std::sync::Once;

use no_std_net::SocketAddr;
use toad::net::Addrd;
use toad::platform::Platform;
use toad_jni::java::{self, Object};
use toad_msg::alloc::Message;
use toad_msg::{Code, Id, Token, Type};

use crate::runtime::Runtime;
use crate::runtime_config::RuntimeConfig;

pub fn runtime_init<'a>() -> (Runtime, java::Env<'a>) {
  let mut _env = crate::test::init();
  let env = &mut _env;

  let cfg = RuntimeConfig::new(env);
  let runtime = Runtime::get_or_init(env, cfg);
  (runtime, _env)
}

fn runtime_poll_req(runtime: &Runtime, env: &mut java::Env) {
  assert!(runtime.poll_req(env).is_none());

  let client = crate::Runtime::try_new("0.0.0.0:5684", Default::default()).unwrap();
  let request = Message::new(Type::Con, Code::GET, Id(0), Token(Default::default()));
  client.send_msg(Addrd(request, "0.0.0.0:5683".parse().unwrap()))
        .unwrap();

  assert!(runtime.poll_req(env).is_some());
}

#[test]
fn e2e_test_suite() {
  let (runtime, mut env) = runtime_init();
  runtime_poll_req(&runtime, &mut env);
}
