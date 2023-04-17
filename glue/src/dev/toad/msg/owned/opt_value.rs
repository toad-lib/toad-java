use toad_jni::java;

pub struct OptValue(java::lang::Object);
java::object_newtype!(OptValue);
impl java::Class for OptValue {
  const PATH: &'static str = package!(dev.toad.msg.owned.OptionValue);
}

impl OptValue {
  pub fn bytes(&self, e: &mut java::Env) -> Vec<u8> {
    static BYTES: java::Field<OptValue, Vec<i8>> = java::Field::new("bytes");
    BYTES.get(e, self)
         .into_iter()
         .map(|i| u8::from_be_bytes(i.to_be_bytes()))
         .collect()
  }
}
