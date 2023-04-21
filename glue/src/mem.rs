use std::sync::Mutex;

use toad::net::Addrd;
use toad_msg::alloc::Message;

use crate::runtime;

/// global [`RuntimeAllocator`] implementation
pub type Shared = GlobalStatic;

/// Trait managing the memory region(s) which java will store pointers to
pub trait SharedMemoryRegion: core::default::Default + core::fmt::Debug + Copy {
  /// Allocate memory for a new runtime instance, yielding a stable pointer to it
  unsafe fn add_runtime(r: crate::Runtime) -> *mut crate::Runtime;

  /// Pass ownership of a [`Message`] to the shared memory region,
  /// yielding a stable pointer to this message.
  unsafe fn alloc_message(m: Addrd<Message>) -> *mut Addrd<Message>;

  /// Delete a message from the shared memory region.
  unsafe fn dealloc_message(m: *mut Addrd<Message>);

  /// Teardown
  unsafe fn dealloc();

  unsafe fn shared_region() -> *mut u8;

  /// Coerce a `long` rep of a pointer to some data within the
  /// shared memory region.
  unsafe fn deref<T>(addr: i64) -> *mut T {
    Self::shared_region().with_addr(addr as usize).cast::<T>()
  }
}

static mut MEM: Mem = Mem { runtimes: vec![],
                            messages: vec![],
                            messages_lock: Mutex::new(()),
                            runtimes_lock: Mutex::new(()) };

struct Mem {
  runtimes: Vec<crate::Runtime>,
  messages: Vec<Addrd<Message>>,

  // These locks don't provide any guarantees that message pointers will
  // stay valid or always point to the correct location, but it does
  // ensure we don't accidentally yield the wrong pointer from `alloc_message`
  // or delete the wrong message in `dealloc_message`.
  messages_lock: Mutex<()>,
  runtimes_lock: Mutex<()>,
}

#[derive(Default, Debug, Clone, Copy)]
pub struct GlobalStatic;
impl SharedMemoryRegion for GlobalStatic {
  unsafe fn dealloc() {
    MEM.runtimes = vec![];
    MEM.messages = vec![];
  }

  unsafe fn add_runtime(r: crate::Runtime) -> *mut crate::Runtime {
    let Mem { ref mut runtimes,
              ref mut runtimes_lock,
              .. } = &mut MEM;
    let _lock = runtimes_lock.lock();
    runtimes.push(r);
    let len = runtimes.len();
    &mut runtimes[len - 1] as _
  }

  unsafe fn alloc_message(m: Addrd<Message>) -> *mut Addrd<Message> {
    let Mem { ref mut messages,
              ref mut messages_lock,
              .. } = &mut MEM;
    let _lock = messages_lock.lock();
    messages.push(m);
    let len = messages.len();
    &mut messages[len - 1] as _
  }

  unsafe fn dealloc_message(m: *mut Addrd<Message>) {
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
