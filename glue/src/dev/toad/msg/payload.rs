use toad_jni::java;

use super::option::ContentFormat;

pub struct Payload(java::lang::Object);
java::object_newtype!(Payload);
impl java::Class for Payload {
  const PATH: &'static str = package!(dev.toad.msg.Payload);
}

impl Payload {
  pub fn new(e: &mut java::Env, bytes: impl IntoIterator<Item = u8>) -> Self {
    static CTOR: java::Constructor<Payload, fn(Vec<i8>)> = java::Constructor::new();
    CTOR.invoke(e,
                bytes.into_iter()
                     .map(|u| i8::from_be_bytes(u.to_be_bytes()))
                     .collect())
  }

  pub fn new_content_format(e: &mut java::Env,
                            bytes: impl IntoIterator<Item = u8>,
                            f: ContentFormat)
                            -> Self {
    static CTOR: java::Constructor<Payload, fn(ContentFormat, Vec<i8>)> = java::Constructor::new();
    CTOR.invoke(e,
                f,
                bytes.into_iter()
                     .map(|u| i8::from_be_bytes(u.to_be_bytes()))
                     .collect())
  }

  pub fn content_format(&self, e: &mut java::Env) -> Option<ContentFormat> {
    static CONTENT_FORMAT: java::Method<Payload, fn() -> java::util::Optional<ContentFormat>> =
      java::Method::new("contentFormat");
    CONTENT_FORMAT.invoke(e, self).to_option(e)
  }

  pub fn bytes(&self, e: &mut java::Env) -> Vec<u8> {
    static BYTES: java::Method<Payload, fn() -> Vec<i8>> = java::Method::new("bytes");
    BYTES.invoke(e, self)
         .into_iter()
         .map(|i| u8::from_be_bytes(i.to_be_bytes()))
         .collect()
  }
}
