use jni::objects::{JObject, JString, JValueGen};
use jni::sys::jstring;
use jni::JNIEnv;
use toad_jni::Sig;
use toad_msg::Type;

pub struct MessageType<'local>(pub JObject<'local>);
impl<'local> MessageType<'local> {
  const ID: &'static str = package!(dev.toad.msg.MessageType);
  const FROM_STRING: Sig = Sig::new().arg(Sig::class("java/lang/String"))
                                     .returning(Sig::class(Self::ID));

  pub fn new(env: &mut JNIEnv<'local>, ty: Type) -> Self {
    let str = env.new_string(match ty {
                               | Type::Con => "CON",
                               | Type::Non => "NON",
                               | Type::Ack => "ACK",
                               | Type::Reset => "RESET",
                             })
                 .unwrap();

    let o = env.call_static_method(Self::ID,
                                   "fromString",
                                   Self::FROM_STRING,
                                   &[JValueGen::Object(&str)])
               .unwrap()
               .l()
               .unwrap();

    Self(o)
  }
}
