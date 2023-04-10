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

  unsafe fn shared_region(addr: i64) -> *mut u8;

  /// Coerce a `long` rep of a pointer to some data within the
  /// shared memory region.
  unsafe fn deref<T>(shared_region_addr: i64, addr: i64) -> *mut T {
    Self::shared_region(shared_region_addr).with_addr(addr as usize)
                                           .cast::<T>()
  }
}

static mut MEM: *mut Mem = core::ptr::null_mut();

struct Mem {
  runtime: crate::Runtime,
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
  unsafe fn dealloc() {
    if !MEM.is_null() {
      drop(Box::from_raw(MEM));
    }
  }

  unsafe fn init(r: impl FnOnce() -> crate::Runtime) -> *mut crate::Runtime {
    if MEM.is_null() {
      MEM = Box::into_raw(Box::new(Mem { runtime: r(),
                                         messages: vec![],
                                         messages_lock: Mutex::new(()) }));
    }

    &mut (*MEM).runtime as _
  }

  unsafe fn alloc_message(m: Message) -> *mut Message {
    let _lock = (*MEM).messages_lock.lock();
    (*MEM).messages.push(m);
    &mut (*MEM).messages[(*MEM).messages.len() - 1] as _
  }

  unsafe fn dealloc_message(m: *mut Message) {
    let _lock = (*MEM).messages_lock.lock();
    let ix = m.offset_from((*MEM).messages.as_slice().as_ptr());
    if ix.is_negative() {
      panic!()
    }

    (*MEM).messages.remove(ix as usize);
  }

  unsafe fn shared_region(_: i64) -> *mut u8 {
    MEM as _
  }
}
