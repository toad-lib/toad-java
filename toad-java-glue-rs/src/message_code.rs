use jni::objects::{JClass, JObject};
use jni::sys::jobject;
use jni::JNIEnv;
use toad_jni::Sig;
use toad_msg::Code;

pub struct MessageCode<'local>(pub JObject<'local>);
impl<'local> MessageCode<'local> {
  const ID: &'static str = package!(dev.toad.msg.MessageCode);
  const CTOR: Sig = Sig::new().arg(Sig::INT).arg(Sig::INT).returning(Sig::VOID);

  pub fn new(env: &mut JNIEnv<'local>, code: Code) -> Self {
    let o = env.new_object(Self::ID,
                           Self::CTOR,
                           &[i32::from(code.class).into(), i32::from(code.detail).into()])
               .unwrap();
    Self(o)
  }
}
