package com.twitter.scrooge

import java.lang.reflect.Method
import org.apache.thrift.protocol.TField
import scala.reflect.ClassTag

/**
 * An abstract class for generic introspection on ThriftStruct classes.
 */
abstract class ThriftStructMetaData[T <: ThriftStruct] {

  /**
   * The '(en)coder-decoder', hence 'codec', which knows
   * how to convert the ThriftStruct from and to its
   * wire representation.
   */
  def codec: ThriftStructCodec[T]

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  def codecClass: Class[_]

  /**
   * The fully qualified name of the ThriftStruct subclass.
   */
  def structClassName: String

  /**
   * Gets the unqualified name of the struct.
   */
  def structName: String

  /**
   * The Class object for ThriftStruct subclass.
   *
   * For a union, this is the parent trait of all branches for the union.
   */
  def structClass: Class[T]

  /**
   * A Seq of ThriftStructFields representing the fields defined in the ThriftStruct.
   *
   * For unions, this will return an empty Seq.
   */
  def fields: Seq[ThriftStructField[T]]

  /**
   * For non-unions, will return its [[ThriftStructFieldInfo ThriftStructFieldInfos]].
   *
   * For unions, will return an empty `Seq`.
   */
  def fieldInfos: Seq[ThriftStructFieldInfo]

  /**
   * For unions, will return its [[ThriftUnionFieldInfo ThriftUnionFieldInfos]].
   *
   * For non-unions, will return an empty `Seq`.
   */
  def unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]]

  /**
   * Parsed annotations at the struct or union level. Left hand side of equal sign is the key,
   * right side is the value.
   */
  def structAnnotations: Map[String, String]
}

object ThriftStructMetaData {

  /**
   * Constructs an implementation of [[ThriftStructMetaData]] that uses reflection
   * to discover field and annotation information.
   */
  def apply[T <: ThriftStruct](codec: ThriftStructCodec[T]): ThriftStructMetaData[T] =
    new ReflectionThriftStructMetaData(codec)

  /**
   * Constructs an implementation of [[ThriftStructMetaData]] that uses the passed in
   * arguments for field and annotation information.
   */
  def apply[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    fields: Seq[ThriftStructField[T]],
    fieldInfos: Seq[ThriftStructFieldInfo],
    unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]],
    structAnnotations: Map[String, String]
  ): ThriftStructMetaData[T] =
    new ConcreteThriftStructMetaData(codec, fields, fieldInfos, unionFields, structAnnotations)

}

/**
 * An implmentation of [[ThriftStructMetaData]] where much of the information
 * is provided via its constructor.
 */
final private[scrooge] class ConcreteThriftStructMetaData[T <: ThriftStruct](
  val codec: ThriftStructCodec[T],
  val fields: Seq[ThriftStructField[T]],
  val fieldInfos: Seq[ThriftStructFieldInfo],
  val unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]],
  val structAnnotations: Map[String, String])
    extends ThriftStructMetaData[T] {

  // The implementation details of this set of values are duplicated between
  // here and `ReflectionThriftStructMetaData`. This is done on purpose to
  // not pollute the abstract `ThriftStructMetaData` class while additional
  // changes are ongoing.
  private[this] val metaDataUtil = new ThriftStructMetaDataUtil[T](codec)
  def codecClass: Class[_] = metaDataUtil.structCodecClass
  def structClassName: String = metaDataUtil.thriftStructSubClassName
  def structName: String = metaDataUtil.thriftStructName
  def structClass: Class[T] = metaDataUtil.thriftStructClass
}

/**
 * An implementation of [[ThriftStructMetaData]] where much of the information
 * is provided via reflection.
 */
final private[scrooge] class ReflectionThriftStructMetaData[T <: ThriftStruct](
  val codec: ThriftStructCodec[T])
    extends ThriftStructMetaData[T] {

  private[this] val metaDataUtil = new ThriftStructMetaDataUtil[T](codec)
  def codecClass: Class[_] = metaDataUtil.structCodecClass
  def structClassName: String = metaDataUtil.thriftStructSubClassName
  def structName: String = metaDataUtil.thriftStructName
  def structClass: Class[T] = metaDataUtil.thriftStructClass

  val fields: Seq[ThriftStructField[T]] =
    PopulateMetaDataWithReflection.getFieldsWithReflection[T](codec, metaDataUtil)
  val fieldInfos: Seq[ThriftStructFieldInfo] =
    PopulateMetaDataWithReflection.getFieldInfosWithReflection[T](codec, metaDataUtil)
  val unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]] =
    PopulateMetaDataWithReflection.getUnionFieldsWithReflection[T](codec, metaDataUtil)
  val structAnnotations: Map[String, String] =
    PopulateMetaDataWithReflection.getStructAnnotationsWithReflection[T](codec, metaDataUtil)
}

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
  UnionFieldType <: ThriftUnion with ThriftStruct: ClassTag,
  ContainedType: ClassTag
](
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
        s"Mismatch between UnionFieldType $fieldClassTag and ContainedType $containedClassTag"
      )
    }
  }
}

/**
 * Field information to be embedded in a generated struct's companion class.
 * Allows for reflection on field types.
 *
 * @param tfield Metadata associated with the field
 * @param isOptional The field is optional
 * @param isRequired The field is required
 * @param manifest The opaque type descriptor for the field type
 * @param keyManifest The opaque type descriptor for the field key type
 * @param valueManifest The opaque type descriptor for the field value type
 * @param typeAnnotations Annotations associated with the type
 * @param fieldAnnotations Annotations associated with the value
 * @param defaultValue Default value if specified
 * @param unsafeEmptyValue Temporary use only. please do not rely on this field
 */
final class ThriftStructFieldInfo(
  val tfield: TField,
  val isOptional: Boolean,
  val isRequired: Boolean,
  val manifest: Manifest[_],
  val keyManifest: scala.Option[Manifest[_]],
  val valueManifest: scala.Option[Manifest[_]],
  val typeAnnotations: Map[String, String],
  val fieldAnnotations: Map[String, String],
  val defaultValue: Option[Any],
  val unsafeEmptyValue: Option[Any]) {

  /**
   * Provide backwards compatibility for older scrooge-generator that does not generate the defaultValue field
   */
  def this(
    tfield: TField,
    isOptional: Boolean,
    isRequired: Boolean,
    manifest: Manifest[_],
    keyManifest: scala.Option[Manifest[_]],
    valueManifest: scala.Option[Manifest[_]],
    typeAnnotations: Map[String, String],
    fieldAnnotations: Map[String, String]
  ) =
    this(
      tfield,
      isOptional,
      isRequired,
      manifest,
      keyManifest,
      valueManifest,
      typeAnnotations,
      fieldAnnotations,
      None,
      None
    )

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
    fieldAnnotations,
    None,
    None
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
      Map.empty[String, String],
      None,
      None
    )
}

/**
 * A simple class for introspecting a ThriftStruct to get its name and class
 * information. It utilizes reflection to get the required information from the
 * codec so usage should be avoided as much as possible.
 */
private final class ThriftStructMetaDataUtil[T <: ThriftStruct](codec: ThriftStructCodec[T]) {

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  def structCodecClass: Class[_] = codec.getClass

  /**
   * The fully qualified name of the ThriftStruct subclass.
   */
  val thriftStructSubClassName: String =
    structCodecClass.getName.dropRight(1) // drop '$' from object name

  /**
   * Gets the unqualified name of the struct.
   */
  val thriftStructName: String = thriftStructSubClassName.split("\\.").last

  /**
   * The Class object for ThriftStruct subclass.
   *
   * For a union, this is the parent trait of all branches for the union.
   */
  val thriftStructClass: Class[T] =
    structCodecClass.getClassLoader.loadClass(thriftStructSubClassName).asInstanceOf[Class[T]]

  def isUnion(): Boolean = classOf[ThriftUnion].isAssignableFrom(thriftStructClass)
}

/**
 * An object containing static methods that retrieves metadata through reflection.
 */
private object PopulateMetaDataWithReflection {

  /**
   * Using reflection, get a Seq of [[ThriftStructField]]s representing
   * the fields defined in the ThriftStruct. This will return an
   * empty Seq for unions.
   */
  def getFieldsWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Seq[ThriftStructField[T]] =
    if (metaDataUtil.isUnion()) {
      Nil
    } else {
      metaDataUtil.structCodecClass.getMethods.toList
        .filter { m => m.getParameterTypes.length == 0 && m.getReturnType == classOf[TField] }
        .map { m =>
          val tfield = m.invoke(codec).asInstanceOf[TField]
          val manifest: scala.Option[Manifest[_]] =
            try {
              Some {
                metaDataUtil.structCodecClass
                  .getMethod(m.getName + "Manifest")
                  .invoke(codec)
                  .asInstanceOf[Manifest[_]]
              }
            } catch { case _: Throwable => None }
          new ThriftStructField[T](tfield, manifest, metaDataUtil.thriftStructClass) {
            def getValue[R](struct: T): R = method.invoke(struct).asInstanceOf[R]
          }
        }
    }

  /**
   * Using reflection, get a Seq of [[ThriftStructFieldInfo]]s
   * representing the fields defined in the ThriftStruct.
   * This will return an empty Seq for unions.
   */
  def getFieldInfosWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Seq[ThriftStructFieldInfo] =
    if (metaDataUtil.isUnion()) {
      Nil
    } else {
      metaDataUtil.structCodecClass
        .getMethod("fieldInfos")
        .invoke(codec)
        .asInstanceOf[Seq[ThriftStructFieldInfo]]
    }

  /**
   * Using reflection, get a Seq of [[ThriftUnionFieldInfo]]s representing the
   * fields of the union. This will return an empty Seq for non-unions.
   */
  def getUnionFieldsWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]] =
    if (!metaDataUtil.isUnion()) {
      Nil
    } else {
      metaDataUtil.structCodecClass
        .getMethod("fieldInfos")
        .invoke(codec)
        .asInstanceOf[Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]]]
    }

  /**
   * Using reflection, get the parsed annotations at the struct or union level.
   * Left hand side of equal sign is the key, right side is the value.
   */
  def getStructAnnotationsWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Map[String, String] =
    metaDataUtil.structCodecClass
      .getMethod("structAnnotations")
      .invoke(codec)
      .asInstanceOf[Map[String, String]]
}
