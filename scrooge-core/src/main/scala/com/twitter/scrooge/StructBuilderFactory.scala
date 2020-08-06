package com.twitter.scrooge

/**
 * A trait that provides an interface for building a new StructBuilder[T].
 *
 * When added to the companion object, it makes it possible to create a T statically
 * without needing to call `newBuilder()` on an instance. For example, calling
 * `Struct.newBuilder()`. In this case, there will be no default values set for each
 * field, making it necessary for the caller to set all the fields in the struct.
 */
trait StructBuilderFactory[T <: ThriftStruct] {

  /**
   * A builder to create a new instance of T.
   *
   * For default values:
   * - Call `newBuilder()` on an instance of the struct
   * - Set an individual field in the struct with `builder.setField(index, value)` while
   *   all other fields will be the same as the instance on which `newBuilder()` was called
   * - Set all the fields in the struct with `builder.setAllFields(seqOfValues)`
   *
   * For a static builder without any default values:
   * - Call `newBuilder()` on the struct object (i.e. `T.newBuilder()`)
   * - Set an individual field in the struct with `builder.setField(index, value)`. No
   *   other fields will be set so it is imperative that the caller sets all of the struct
   *   fields manually with `setField` or `setAllFields`
   * - Set all the fields in the struct with `builder.setAllFields(seqOfValues)`
   *
   * Finally, call `builder.build()` to build the new T.
   */
  def newBuilder(): StructBuilder[T]
}
