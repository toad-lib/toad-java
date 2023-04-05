use jni::{JNIEnv, objects::{JObject, JByteArray}};
use toad_jni::Sig;

pub mod path {
  pub const U64: &'static str = package!(dev.toad.ffi.u64);
  pub const U32: &'static str = package!(dev.toad.ffi.u32);
  pub const U16: &'static str = package!(dev.toad.ffi.u16);
  pub const U8: &'static str = package!(dev.toad.ffi.u8);
}

pub fn u64<'a>(e: &mut JNIEnv<'a>, o: JObject<'a>) -> u64 {
  let bi = e.call_method(o, "bigintValue", Sig::new().returning(Sig::class("java.math.BigInteger")), &[]).unwrap().l().unwrap();
  let barr: JByteArray<'a> = e.call_method(bi, "toByteArray", Sig::new().returning(Sig::array_of(Sig::BYTE)), &[]).unwrap().l().unwrap().try_into().unwrap();

  let mut bytes = [0i8; 8];

  // BigInteger is a growable two's complement integer
  //
  // the "growable" comes from its backing structure being a simple
  // int array `int[]`, where bytes are added as needed to afford capacity.
  //
  // two's-complement means the most significant bit (the first bit of the first byte)
  // indicates the sign of the integer, where 0 is positive and 1 is negative.
  //
  // The rest of the bits are unchanged, meaning the range is from `-(2^(n - 1))`
  // to `2^(n - 1) - 1`.
  //
  // For example, a two's complement i8 would be able to represent `-128` (`0b11111111`),
  // `0` (`0b00000000`) to `127` (`0b01111111`). for positive integers, the representation is
  // the same as unsigned integers, meaning we simply need to make sure we don't accidentally
  // interpret the first bit as part of the integer.
  //
  // Here we assume whoever is responsible for BigInteger made sure that it's positive,
  // so converting the big-endian two's complement int
  e.get_byte_array_region(&barr, 0, &mut bytes).unwrap();

  // https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/math/BigInteger.html#toByteArray()
  //
  // BigInt.toByteArray actually returns the raw byte representation of the integer, NOT
  // two's complement `byte`s as the type signature would lead you to believe.
  //
  // To interpret these bytes as i8 is incorrect.
  let bytes = bytes.map(|i| i8::to_be_bytes(i)[0]);

  u64::from_be_bytes(bytes)
}

pub fn u32<'a>(e: &mut JNIEnv<'a>, o: JObject<'a>) -> u32 {
  e.call_method(o, "longValue", Sig::new().returning(Sig::LONG), &[]).unwrap().j().unwrap() as u32
}

pub fn u16<'a>(e: &mut JNIEnv<'a>, o: JObject<'a>) -> u16 {
  e.call_method(o, "intValue", Sig::new().returning(Sig::INT), &[]).unwrap().i().unwrap() as u16
}

pub fn u8<'a>(e: &mut JNIEnv<'a>, o: JObject<'a>) -> u8 {
  e.call_method(o, "shortValue", Sig::new().returning(Sig::SHORT), &[]).unwrap().s().unwrap() as u8
}
