use toad_jni::java;

use crate::dev::toad::ffi;

pub struct Id(java::lang::Object);
java::object_newtype!(Id);
impl java::Class for Id {
  const PATH: &'static str = package!(dev.toad.msg.Id);
}

impl Id {
  pub fn from_u16(e: &mut java::Env, id: u16) -> Self {
    static CTOR: java::Constructor<Id, fn(i32)> = java::Constructor::new();
    CTOR.invoke(e, id.into())
  }

  pub fn to_u16(&self, e: &mut java::Env) -> u16 {
    static ID: java::Field<Id, ffi::u16> = java::Field::new("id");
    ID.get(e, self).to_rust(e)
  }

  pub fn from_toad(e: &mut java::Env, toad: toad_msg::Id) -> Self {
    Self::from_u16(e, toad.0)
  }

  pub fn to_toad(&self, e: &mut java::Env) -> toad_msg::Id {
    toad_msg::Id(self.to_u16(e))
  }
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn roundtrip() {
    let mut e = crate::test::init();
    let e = &mut e;

    let toad = toad_msg::Id(444);
    let toadj = Id::from_toad(e, toad);
    assert_eq!(toadj.to_toad(e), toad);
  }
}
