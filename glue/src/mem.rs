/// global [`RuntimeAllocator`] implementation
pub type Runtime = RuntimeGlobalStaticAllocator;

/// Trait managing the memory region(s) associated with the toad runtime
/// data structure.
///
/// Notably, any and all references produced by the runtime will be to data
/// within the Runtime's memory region, meaning that we can easily leverage
/// strict provenance to prevent addresses from leaking outside of that memory region.
pub trait RuntimeAllocator: core::default::Default + core::fmt::Debug + Copy {
  /// Allocate memory for the runtime and yield a stable pointer to it
  unsafe fn alloc(r: crate::Runtime) -> *mut crate::Runtime;

  /// Coerce a `long` rep of the stable pointer created by [`Self::alloc`] to
  /// a pointer (preferably using strict_provenance)
  unsafe fn deref(addr: i64) -> *mut crate::Runtime;

  /// Coerce a `long` rep of a pointer to some data within the
  /// Runtime data structure.
  ///
  /// Requires the Runtime address in order for the new pointer
  /// to inherit its provenance.
  unsafe fn deref_inner<T>(runtime_addr: i64, addr: i64) -> *mut T {
    Self::deref(runtime_addr).with_addr(addr as usize)
                             .cast::<T>()
  }
}

static mut RUNTIME: *mut crate::Runtime = core::ptr::null_mut();

#[derive(Default, Debug, Clone, Copy)]
pub struct RuntimeGlobalStaticAllocator;
impl RuntimeAllocator for RuntimeGlobalStaticAllocator {
  /// Nops on already-init
  unsafe fn alloc(r: crate::Runtime) -> *mut crate::Runtime {
    if RUNTIME.is_null() {
      let p =
        std::alloc::alloc(std::alloc::Layout::new::<crate::Runtime>()).cast::<crate::Runtime>();
      *p = r;
      RUNTIME = p;
      RUNTIME
    } else {
      RUNTIME
    }
  }

  unsafe fn deref(_: i64) -> *mut crate::Runtime {
    RUNTIME
  }
}
