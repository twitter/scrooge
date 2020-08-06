package com.twitter.scrooge

import scala.reflect.ClassTag

/**
 * A class that provides an interface for building a new ThriftStruct from an existing
 * ThriftStruct or statically from T.
 *
 * We pass in a list of [[ClassTag]]s which describe each of the struct's field types
 * so that we can validate the values we are setting at runtime.
 */
abstract class StructBuilder[T <: ThriftStruct](fieldTypes: IndexedSeq[ClassTag[_]]) {
  protected val fieldArray: Array[Any] = new Array[Any](fieldTypes.size)

  /**
   * Add or update the field in the fieldArray.
   *
   * @param index the index of the field to add or update.
   * @param v the value of the field to add or update.
   * @param tag the class tag of the value to add or update.
   */
  private[this] def addOrUpdateFieldArray[A](index: Int, v: Any)(implicit tag: ClassTag[A]): Unit =
    v match {
      case inputValue: A => fieldArray(index) = inputValue
      case _ => throw new IllegalArgumentException(s"value at index $index must be of type $tag")
    }

  /**
   * The error message used when `build()` throws an InvalidFieldsException when
   * values are not present in the fieldArray or when the values are invalid.
   *
   * @param structName the name of the ThriftStruct to include in the error message.
   */
  protected def structBuildError(structName: String): String =
    s"All fields must be set using `StructBuilder[$structName].setField` " +
      s"or `StructBuilder[$structName].setAllFields` before calling " +
      s"`StructBuilder[$structName].build`."

  /**
   * Set the field at `index` with `value`.
   *
   * When called on a builder that was created from an existing instance of T,
   * all the other fields will be the same as that struct.
   *
   * When called on a static builder, the other fields will not have defaults so
   * the caller must ensure that all the other fields are set before calling `build()`.
   *
   * @param index the index of the field list for the ThriftStruct.
   * @param value the value to use for the field in ThriftStruct.
   */
  def setField(index: Int, value: Any): StructBuilder[T] = {
    addOrUpdateFieldArray(index, value)(fieldTypes(index))
    this
  }

  /**
   * Set all of the fields of the new ThriftStruct. A value must be present for
   * all fields required to construct the ThriftStruct and in the correct order.
   *
   * @param fields a sequence of values ordered by field index.
   */
  def setAllFields(fields: Seq[Any])(implicit tag: ClassTag[T]): StructBuilder[T] = {
    if (fields.size != fieldTypes.size) {
      throw new IndexOutOfBoundsException(
        s"The input must have ${fieldTypes.size} " +
          s"value(s), found ${fields.size}.")
    } else {
      fields.zipWithIndex.foreach {
        case (value, index) =>
          addOrUpdateFieldArray(index, value)(fieldTypes(index))
      }
    }
    this
  }

  /**
   * Build a new ThriftStruct after setting the desired fields.
   */
  def build(): T
}
