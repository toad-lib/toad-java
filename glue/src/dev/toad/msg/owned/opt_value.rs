use toad_jni::java;

pub struct OptValue(java::lang::Object);
java::object_newtype!(OptValue);
impl java::Class for OptValue {
  const PATH: &'static str = package!(dev.toad.msg.owned.OptionValue);
}

impl OptValue {
  pub fn new(e: &mut java::Env, bytes: impl IntoIterator<Item = u8>) -> Self {
    static CTOR: java::Constructor<OptValue, fn(Vec<i8>)> = java::Constructor::new();
    CTOR.invoke(e,
                bytes.into_iter()
                     .map(|b| i8::from_be_bytes(b.to_be_bytes()))
                     .collect())
  }

  pub fn bytes(&self, e: &mut java::Env) -> Vec<u8> {
    static BYTES: java::Field<OptValue, Vec<i8>> = java::Field::new("bytes");
    BYTES.get(e, self)
         .into_iter()
         .map(|i| u8::from_be_bytes(i.to_be_bytes()))
         .collect()
  }
}
