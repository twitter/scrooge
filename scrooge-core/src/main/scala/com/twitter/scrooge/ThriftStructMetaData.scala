package com.twitter.scrooge

import java.lang.reflect.Method
import org.apache.thrift.protocol.TField
import scala.reflect.ClassTag

/**
 * A simple class for generic introspection on ThriftStruct classes.
 */
final class ThriftStructMetaData[T <: ThriftStruct](val codec: ThriftStructCodec[T]) {
  private[this] def toCamelCase(str: String): String = {
    str.takeWhile(_ == '_') + str.
      split('_').
      filterNot(_.isEmpty).
      zipWithIndex.map { case (part, ind) =>
        val first = if (ind == 0) part.charAt(0).toLower else part.charAt(0).toUpper
        val isAllUpperCase = part.forall(_.isUpper)
        val rest = if (isAllUpperCase) part.drop(1).toLowerCase else part.drop(1)
        new StringBuilder(part.length).append(first).append(rest)
      }.mkString
  }

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  def codecClass: Class[_] = codec.getClass

  /**
   * The fully qualified name of the ThriftStruct subclass.
   */
  val structClassName: String = codecClass.getName.dropRight(1) // drop '$' from object name

  /**
   * Gets the unqualified name of the struct.
   */
  val structName: String = structClassName.split("\\.").last

  /**
   * The Class object for ThriftStruct subclass.
   *
   * For a union, this is the parent trait of all branches for the union.
   */
  val structClass: Class[T] =
    codecClass.getClassLoader.loadClass(structClassName).asInstanceOf[Class[T]]

  private[this] val isUnion: Boolean =
    classOf[ThriftUnion].isAssignableFrom(structClass)

  /**
   * A Seq of ThriftStructFields representing the fields defined in the ThriftStruct.
   *
   * For unions, this will return an empty Seq.
   */
  val fields: Seq[ThriftStructField[T]] = {
    if (isUnion) {
      Nil
    } else {
      codecClass.getMethods.toList.filter { m =>
        m.getParameterTypes.length == 0 && m.getReturnType == classOf[TField]
      }.map { m =>
        val tfield = m.invoke(codec).asInstanceOf[TField]
        val manifest: scala.Option[Manifest[_]] = try {
          Some {
            codecClass
              .getMethod(m.getName + "Manifest")
              .invoke(codec)
              .asInstanceOf[Manifest[_]]
          }
        } catch { case _: Throwable => None }
        val method = structClass.getMethod(toCamelCase(tfield.name))
        new ThriftStructField[T](tfield, method, manifest)
      }
    }
  }

  /**
   * For unions, will return its [[ThriftUnionFieldInfo ThriftUnionFieldInfos]].
   *
   * For non-unions, will return an empty `Seq`.
   */
  val unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]] = {
    if (!isUnion) {
      Nil
    } else {
      codecClass.getMethod("fieldInfos")
        .invoke(codec)
        .asInstanceOf[Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]]]
    }
  }

}

final class ThriftStructField[T <: ThriftStruct](
    val tfield: TField,
    val method: Method,
    val manifest: scala.Option[Manifest[_]]) {

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
  def getValue[R](struct: T): R = method.invoke(struct).asInstanceOf[R]
}

/**
 * Field information to be embedded in a generated union's companion class.
 * @param structFieldInfo Nested [[ThriftStructFieldInfo]] containing additional details about union
 *                        field
 * @param fieldUnapply The unapply method on the companion object of the union field class. Used to
 *                     easily extract the field value from an instance of the union field class.
 * @tparam UnionFieldType The type of the union field represented by this class
 * @tparam ContainedType The type of the value contained in the union field represented by this
 *                       class
 */
final class ThriftUnionFieldInfo[
    UnionFieldType <: ThriftUnion with ThriftStruct : ClassTag,
    ContainedType: ClassTag](
    val structFieldInfo: ThriftStructFieldInfo,
    fieldUnapply: UnionFieldType => scala.Option[ContainedType]) {

  /**
   * Class tag for the class representing this union field; useful for reflection-related tasks
   */
  val fieldClassTag: ClassTag[UnionFieldType] = implicitly[ClassTag[UnionFieldType]]

  private[this] val containedClassTag: ClassTag[ContainedType] = implicitly[ClassTag[ContainedType]]

  /**
   * Extracts the value contained within an instance of this union's class
   * @param field The field instance from which to extract the contained value
   * @return The extracted value
   */
  def fieldValue(field: ThriftStruct with ThriftUnion): ContainedType = {
    fieldUnapply(field.asInstanceOf[UnionFieldType]).getOrElse {
      throw new IllegalStateException(
        s"Mismatch between UnionFieldType $fieldClassTag and ContainedType $containedClassTag")
    }
  }
}

/**
 * Field information to be embedded in a generated struct's companion class.
 * Allows for reflection on field types.
 */
final class ThriftStructFieldInfo(
  val tfield: TField,
  val isOptional: Boolean,
  val isRequired: Boolean,
  val manifest: Manifest[_],
  val keyManifest: scala.Option[Manifest[_]],
  val valueManifest: scala.Option[Manifest[_]],
  val typeAnnotations: Map[String, String],
  val fieldAnnotations: Map[String, String]
) {
  /**
   * Provide backwards compatibility for older scrooge-generator that does not generate the isRequired flag
   */
  def this(
    tfield: TField,
    isOptional: Boolean,
    manifest: Manifest[_],
    keyManifest: scala.Option[Manifest[_]],
    valueManifest: scala.Option[Manifest[_]],
    typeAnnotations: Map[String, String],
    fieldAnnotations: Map[String, String]
  ) = this(
    tfield,
    isOptional,
    !isOptional,
    manifest,
    keyManifest,
    valueManifest,
    typeAnnotations,
    fieldAnnotations
  )

  /**
   * Secondary constructor provided for backwards compatibility:
   * Older scrooge-generator does not produce annotations.
   */
  def this(
    tfield: TField,
    isOptional: Boolean,
    manifest: Manifest[_],
    keyManifest: scala.Option[Manifest[_]],
    valueManifest: scala.Option[Manifest[_]]
  ) =
    this(
      tfield,
      isOptional,
      !isOptional,
      manifest,
      keyManifest,
      valueManifest,
      Map.empty[String, String],
      Map.empty[String, String]
    )
}

