use tinyvec::ArrayVec;
use toad_jni::java;

use crate::dev::toad::ffi;

pub struct Token(java::lang::Object);
java::object_newtype!(Token);
impl java::Class for Token {
  const PATH: &'static str = package!(dev.toad.msg.Token);
}

impl Token {
  pub fn from_bytes(e: &mut java::Env, bytes: &[u8]) -> Self {
    static CTOR: java::Constructor<Token, fn(Vec<i8>)> = java::Constructor::new();
    CTOR.invoke(e,
                bytes.iter()
                     .copied()
                     .map(|u| i8::from_be_bytes(u.to_be_bytes()))
                     .collect())
  }

  pub fn to_bytes(&self, e: &mut java::Env) -> ArrayVec<[u8; 8]> {
    static BYTES: java::Field<Token, Vec<i8>> = java::Field::new("bytes");
    BYTES.get(e, self)
         .into_iter()
         .map(|i| u8::from_be_bytes(i.to_be_bytes()))
         .collect()
  }

  pub fn from_toad(e: &mut java::Env, token: toad_msg::Token) -> Self {
    Self::from_bytes(e, &token.0)
  }

  pub fn to_toad(&self, e: &mut java::Env) -> toad_msg::Token {
    toad_msg::Token(self.to_bytes(e))
  }
}

#[cfg(test)]
mod tests {
  use tinyvec::array_vec;

  use super::*;

  #[test]
  fn roundtrip() {
    let mut e = crate::test::init();
    let e = &mut e;

    let toad = toad_msg::Token(array_vec![1, 2, 3, 4, 5]);
    let toadj = Token::from_toad(e, toad);
    assert_eq!(toadj.to_toad(e), toad);
  }
}
