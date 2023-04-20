use core::primitive as rust;

use jni::objects::JObject;
use jni::sys::jbyteArray;
use toad_jni::java;

#[allow(non_camel_case_types)]
pub struct u64(java::lang::Object);

java::object_newtype!(u64);
impl java::Class for u64 {
  const PATH: &'static str = package!(dev.toad.ffi.u64);
}

impl u64 {
  pub fn to_rust(&self, e: &mut java::Env) -> rust::u64 {
    static BIGINT_VALUE: java::Method<u64, fn() -> java::math::BigInteger> =
      java::Method::new("bigintValue");
    let bigint = BIGINT_VALUE.invoke(e, self);
    bigint.to_i128(e) as rust::u64
  }

  pub fn from_rust(e: &mut java::Env, u: rust::u64) -> Self {
    static CTOR: java::Constructor<u64, fn(java::math::BigInteger)> = java::Constructor::new();
    let bi = java::math::BigInteger::from_be_bytes(e, &i128::from(u).to_be_bytes());
    CTOR.invoke(e, bi)
  }
}

#[allow(non_camel_case_types)]
pub struct u32(java::lang::Object);

java::object_newtype!(u32);
impl java::Class for u32 {
  const PATH: &'static str = package!(dev.toad.ffi.u32);
}

impl u32 {
  pub fn to_rust(&self, e: &mut java::Env) -> rust::u32 {
    static LONG_VALUE: java::Method<u32, fn() -> i64> = java::Method::new("longValue");
    LONG_VALUE.invoke(e, self) as rust::u32
  }

  pub fn from_rust(e: &mut java::Env, u: rust::u32) -> Self {
    static CTOR: java::Constructor<u32, fn(i64)> = java::Constructor::new();
    CTOR.invoke(e, u.into())
  }
}

#[allow(non_camel_case_types)]
pub struct u16(java::lang::Object);

java::object_newtype!(u16);
impl java::Class for u16 {
  const PATH: &'static str = package!(dev.toad.ffi.u16);
}

impl u16 {
  pub fn to_rust(&self, e: &mut java::Env) -> rust::u16 {
    static INT_VALUE: java::Method<u16, fn() -> i32> = java::Method::new("intValue");
    INT_VALUE.invoke(e, self) as rust::u16
  }

  pub fn from_rust(e: &mut java::Env, u: rust::u16) -> Self {
    static CTOR: java::Constructor<u16, fn(i32)> = java::Constructor::new();
    CTOR.invoke(e, u.into())
  }
}

#[allow(non_camel_case_types)]
pub struct u8(java::lang::Object);

java::object_newtype!(u8);
impl java::Class for u8 {
  const PATH: &'static str = package!(dev.toad.ffi.u8);
}

impl u8 {
  pub fn to_rust(&self, e: &mut java::Env) -> rust::u8 {
    static SHORT_VALUE: java::Method<u8, fn() -> i16> = java::Method::new("shortValue");
    SHORT_VALUE.invoke(e, self) as rust::u8
  }

  pub fn from_rust(e: &mut java::Env, u: rust::u8) -> Self {
    static CTOR: java::Constructor<u8, fn(i16)> = java::Constructor::new();
    CTOR.invoke(e, u.into())
  }
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_ffi_u8_toByte<'local>(mut env: java::Env<'local>,
                                                           u: JObject<'local>)
                                                           -> i8 {
  let u = java::lang::Object::from_local(&mut env, u).upcast_to::<u8>(&mut env);
  i8::from_be_bytes([u.to_rust(&mut env).to_be()])
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_ffi_u16_toBytes<'local>(mut env: java::Env<'local>,
                                                             u: JObject<'local>)
                                                             -> jbyteArray {
  let u = java::lang::Object::from_local(&mut env, u).upcast_to::<u16>(&mut env);
  let bs = u.to_rust(&mut env).to_be_bytes();
  env.byte_array_from_slice(&bs).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_ffi_u32_toBytes<'local>(mut env: java::Env<'local>,
                                                             u: JObject<'local>)
                                                             -> jbyteArray {
  let u = java::lang::Object::from_local(&mut env, u).upcast_to::<u32>(&mut env);
  let bs = u.to_rust(&mut env).to_be_bytes();
  env.byte_array_from_slice(&bs).unwrap().as_raw()
}

#[no_mangle]
pub extern "system" fn Java_dev_toad_ffi_u64_toBytes<'local>(mut env: java::Env<'local>,
                                                             u: JObject<'local>)
                                                             -> jbyteArray {
  let u = java::lang::Object::from_local(&mut env, u).upcast_to::<u64>(&mut env);
  let bs = u.to_rust(&mut env).to_be_bytes();
  env.byte_array_from_slice(&bs).unwrap().as_raw()
}
