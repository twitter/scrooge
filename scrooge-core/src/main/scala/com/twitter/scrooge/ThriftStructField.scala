package com.twitter.scrooge

import java.lang.reflect.Method
import org.apache.thrift.protocol.TField

/**
 * Companion of ThriftStructField that provides helper methods.
 */
private object ThriftStructField {

  /**
   * Convert snake case to dromedary case. Dromedary case is when the
   * first letter of the first word is lowercase, the first letter
   * of following words are uppercase, but the rest are lowercase.
   * In other words, it ensures lowercase between word boundaries, unlike
   * [[com.twitter.conversions.StringOps.toCamelCase()]]. This method
   * covers the following cases:
   *  - "foo_bar" -> "fooBar"
   *  - "FOO_BAR" -> "fooBar"
   *  - "foo_BarBaz" -> "fooBarBaz"
   * Handling these cases is specific to scrooge, which is
   * why it lives here.
   */
  def toDromedaryCase(str: String): String = {
    str.takeWhile(_ == '_') + str
      .split('_')
      .filterNot(_.isEmpty)
      .zipWithIndex
      .map {
        case (part, ind) =>
          val first = if (ind == 0) part.charAt(0).toLower else part.charAt(0).toUpper
          val isAllUpperCase = part.forall(_.isUpper)
          val rest = if (isAllUpperCase) part.drop(1).toLowerCase else part.drop(1)
          new StringBuilder(part.length).append(first).append(rest)
      }
      .mkString
  }
}

/**
 * Field information and a generic way to access the field value. The
 * [[ThriftStructField]] is defined in the generated struct's companion
 * object in order to avoid using reflection to access the field value.
 * If the struct is being created in another way (i.e. not generated through
 * scrooge) the fields will be defined via reflection in
 * [[PopulateMetaDataWithReflection.getFieldsWithReflection]].
 */
abstract class ThriftStructField[T <: ThriftStruct](
  val tfield: TField,
  val manifest: scala.Option[Manifest[_]],
  thriftStructClass: Class[T]) {

  val method: Method =
    thriftStructClass.getMethod(ThriftStructField.toDromedaryCase(tfield.name))

  /**
   * The TField field name, same as the method name on the ThriftStruct for the value.
   */
  def name: String = tfield.name

  /**
   * The TField field id, as defined in the source thrift file.
   */
  def id: Short = tfield.id

  /**
   * The TField field type.  See TType for possible values.
   */
  def `type`: Byte = tfield.`type`

  /**
   * Gets the value of the field from the struct. You can specify the expected return
   * type, rather than casting explicitly.
   */
  def getValue[R](struct: T): R
}
