use core::primitive as rust;

use jni::objects::{GlobalRef, JByteArray, JObject};
use jni::JNIEnv;
use toad_jni::cls::java;
use toad_jni::convert::Object;
use toad_jni::Sig;

#[allow(non_camel_case_types)]
pub struct u64(GlobalRef);
impl u64 {
  pub const PATH: &'static str = package!(dev.toad.ffi.u64);
  pub const CTOR: Sig = Sig::new().arg(Sig::class(java::math::BigInteger::PATH))
                                  .returning(Sig::VOID);
  pub const BIGINT_VALUE: Sig = Sig::new().returning(Sig::class(java::math::BigInteger::PATH));

  pub fn to_rust<'a>(&self, e: &mut JNIEnv<'a>) -> rust::u64 {
    let bi = e.call_method(self.0.as_obj(), "bigintValue", Self::BIGINT_VALUE, &[])
              .unwrap()
              .l()
              .unwrap();
    let bi = e.new_global_ref(bi).unwrap();
    let bi = java::math::BigInteger::from_java(bi);
    bi.to_i128(e) as rust::u64
  }

  pub fn from_rust<'a>(e: &mut JNIEnv<'a>, u: rust::u64) -> Self {
    let bi = java::math::BigInteger::from_be_bytes(e, &i128::from(u).to_be_bytes());
    let bi = e.new_object(Self::PATH, Self::CTOR, &[bi.to_java().as_obj().into()])
              .unwrap();
    Self(e.new_global_ref(bi).unwrap())
  }
}

impl Object for u64 {
  fn from_java(jobj: GlobalRef) -> Self {
    Self(jobj)
  }

  fn to_java(self) -> GlobalRef {
    self.0
  }
}

#[allow(non_camel_case_types)]
pub struct u32(GlobalRef);
impl u32 {
  pub const PATH: &'static str = package!(dev.toad.ffi.u32);
  pub const CTOR: Sig = Sig::new().arg(Sig::LONG).returning(Sig::VOID);
  pub const LONG_VALUE: Sig = Sig::new().returning(Sig::LONG);

  pub fn to_rust<'a>(&self, e: &mut JNIEnv<'a>) -> rust::u32 {
    let long = e.call_method(self.0.as_obj(), "longValue", Self::LONG_VALUE, &[])
                .unwrap()
                .j()
                .unwrap();
    long as rust::u32
  }

  pub fn from_rust<'a>(e: &mut JNIEnv<'a>, u: rust::u32) -> Self {
    let bi = e.new_object(Self::PATH, Self::CTOR, &[rust::i64::from(u).into()])
              .unwrap();
    Self(e.new_global_ref(bi).unwrap())
  }
}

impl Object for u32 {
  fn from_java(jobj: GlobalRef) -> Self {
    Self(jobj)
  }

  fn to_java(self) -> GlobalRef {
    self.0
  }
}

#[allow(non_camel_case_types)]
pub struct u16(GlobalRef);
impl u16 {
  pub const PATH: &'static str = package!(dev.toad.ffi.u16);
  pub const CTOR: Sig = Sig::new().arg(Sig::INT).returning(Sig::VOID);
  pub const INT_VALUE: Sig = Sig::new().returning(Sig::INT);

  pub fn to_rust<'a>(&self, e: &mut JNIEnv<'a>) -> rust::u16 {
    let int = e.call_method(self.0.as_obj(), "intValue", Self::INT_VALUE, &[])
               .unwrap()
               .i()
               .unwrap();
    int as rust::u16
  }

  pub fn from_rust<'a>(e: &mut JNIEnv<'a>, u: rust::u16) -> Self {
    let bi = e.new_object(Self::PATH, Self::CTOR, &[rust::i32::from(u).into()])
              .unwrap();
    Self(e.new_global_ref(bi).unwrap())
  }
}

impl Object for u16 {
  fn from_java(jobj: GlobalRef) -> Self {
    Self(jobj)
  }

  fn to_java(self) -> GlobalRef {
    self.0
  }
}

#[allow(non_camel_case_types)]
pub struct u8(GlobalRef);
impl u8 {
  pub const PATH: &'static str = package!(dev.toad.ffi.u8);
  pub const CTOR: Sig = Sig::new().arg(Sig::SHORT).returning(Sig::VOID);
  pub const SHORT_VALUE: Sig = Sig::new().returning(Sig::SHORT);

  pub fn to_rust<'a>(&self, e: &mut JNIEnv<'a>) -> rust::u8 {
    let int = e.call_method(self.0.as_obj(), "shortValue", Self::SHORT_VALUE, &[])
               .unwrap()
               .s()
               .unwrap();
    int as rust::u8
  }

  pub fn from_rust<'a>(e: &mut JNIEnv<'a>, u: rust::u8) -> Self {
    let bi = e.new_object(Self::PATH, Self::CTOR, &[rust::i16::from(u).into()])
              .unwrap();
    Self(e.new_global_ref(bi).unwrap())
  }
}

impl Object for u8 {
  fn from_java(jobj: GlobalRef) -> Self {
    Self(jobj)
  }

  fn to_java(self) -> GlobalRef {
    self.0
  }
}
