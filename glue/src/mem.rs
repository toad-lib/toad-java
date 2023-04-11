use std::sync::Mutex;

use toad_msg::alloc::Message;

/// global [`RuntimeAllocator`] implementation
pub type Shared = GlobalStatic;

/// Trait managing the memory region(s) which java will store pointers to
pub trait SharedMemoryRegion: core::default::Default + core::fmt::Debug + Copy {
  /// Allocate memory for the runtime and yield a stable pointer to it
  ///
  /// This is idempotent and will only invoke the provided callback if the runtime
  /// has not already been initialized.
  unsafe fn init(r: impl FnOnce() -> crate::Runtime) -> *mut crate::Runtime;

  /// Pass ownership of a [`Message`] to the shared memory region,
  /// yielding a stable pointer to this message.
  unsafe fn alloc_message(m: Message) -> *mut Message;

  /// Delete a message from the shared memory region.
  unsafe fn dealloc_message(m: *mut Message);

  /// Teardown
  unsafe fn dealloc();

  unsafe fn shared_region() -> *mut u8;

  /// Coerce a `long` rep of a pointer to some data within the
  /// shared memory region.
  unsafe fn deref<T>(addr: i64) -> *mut T {
    Self::shared_region().with_addr(addr as usize).cast::<T>()
  }
}

static mut MEM: Mem = Mem { runtime: None,
                            messages: vec![],
                            messages_lock: Mutex::new(()) };

struct Mem {
  runtime: Option<crate::Runtime>,
  messages: Vec<Message>,

  /// Lock used by `alloc_message` and `dealloc_message` to ensure
  /// they are run serially.
  ///
  /// This doesn't provide any guarantees that message pointers will
  /// stay valid or always point to the correct location, but it does
  /// ensure we don't accidentally yield the wrong pointer from `alloc_message`
  /// or delete the wrong message in `dealloc_message`.
  messages_lock: Mutex<()>,
}

#[derive(Default, Debug, Clone, Copy)]
pub struct GlobalStatic;
impl SharedMemoryRegion for GlobalStatic {
  unsafe fn dealloc() {}

  unsafe fn init(r: impl FnOnce() -> crate::Runtime) -> *mut crate::Runtime {
    if MEM.runtime.is_none() {
      MEM.runtime = Some(r());
    }

    MEM.runtime.as_mut().unwrap() as _
  }

  unsafe fn alloc_message(m: Message) -> *mut Message {
    let Mem { ref mut messages,
              ref mut messages_lock,
              .. } = &mut MEM;
    let _lock = messages_lock.lock();
    messages.push(m);
    let len = messages.len();
    &mut messages[len - 1] as _
  }

  unsafe fn dealloc_message(m: *mut Message) {
    let Mem { messages,
              messages_lock,
              .. } = &mut MEM;
    let _lock = messages_lock.lock();
    let ix = m.offset_from(messages.as_slice().as_ptr());
    if ix.is_negative() {
      panic!()
    }

    messages.remove(ix as usize);
  }

  unsafe fn shared_region() -> *mut u8 {
    &mut MEM as *mut Mem as *mut u8
  }
}
