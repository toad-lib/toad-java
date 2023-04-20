pub mod option;
pub mod owned;
pub mod ref_;

mod ty;
use toad_jni::java;
pub use ty::Type;

mod payload;
pub use payload::Payload;

mod code;
pub use code::Code;

mod id;
pub use id::Id;

mod token;
pub use token::Token;

pub struct Message(pub java::lang::Object);
java::object_newtype!(Message);
impl java::Class for Message {
  const PATH: &'static str = package!(dev.toad.msg.Message);
}

pub struct Option(pub java::lang::Object);
java::object_newtype!(Option);
impl java::Class for Option {
  const PATH: &'static str = package!(dev.toad.msg.Option);
}

pub struct OptionValue(pub java::lang::Object);
java::object_newtype!(OptionValue);
impl java::Class for OptionValue {
  const PATH: &'static str = package!(dev.toad.msg.OptionValue);
}
