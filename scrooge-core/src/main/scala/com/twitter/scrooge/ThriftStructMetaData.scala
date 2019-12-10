package com.twitter.scrooge

import java.lang.reflect.Method
import org.apache.thrift.protocol.TField
import scala.reflect.ClassTag

/**
 * A simple class for generic introspection on ThriftStruct classes.
 */
final class ThriftStructMetaData[T <: ThriftStruct] private (
  val codec: ThriftStructCodec[T],
  structFields: Option[Seq[ThriftStructField[T]]],
  structFieldInfos: Option[Seq[ThriftStructFieldInfo]],
  unionFieldInfos: Option[Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]]],
  annotations: Option[Map[String, String]]) {

  /**
   * A constructor that when used, the metadata is
   * populated via reflection.
   */
  def this(codec: ThriftStructCodec[T]) = this(
    codec = codec,
    structFields = None,
    structFieldInfos = None,
    unionFieldInfos = None,
    annotations = None
  )

  /**
   * A constructor that when used, does not use reflection to populate
   * the metadata and rather uses the information that is passed in.
   */
  def this(
    codec: ThriftStructCodec[T],
    structFields: Seq[ThriftStructField[T]],
    structFieldInfos: Seq[ThriftStructFieldInfo],
    unionFieldInfos: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]],
    annotations: Map[String, String]
  ) = this(
    codec = codec,
    structFields = Some(structFields),
    structFieldInfos = Some(structFieldInfos),
    unionFieldInfos = Some(unionFieldInfos),
    annotations = Some(annotations)
  )

  private[this] val metaDataUtil = new ThriftStructMetaDataUtil[T](codec)

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  def codecClass: Class[_] = metaDataUtil.structCodecClass

  /**
   * The fully qualified name of the ThriftStruct subclass.
   */
  val structClassName: String = metaDataUtil.thriftStructSubClassName

  /**
   * Gets the unqualified name of the struct.
   */
  val structName: String = metaDataUtil.thriftStructName

  /**
   * The Class object for ThriftStruct subclass.
   *
   * For a union, this is the parent trait of all branches for the union.
   */
  val structClass: Class[T] = metaDataUtil.thriftStructClass

  /**
   * A Seq of ThriftStructFields representing the fields defined in the ThriftStruct.
   *
   * For unions, this will return an empty Seq.
   */
  val fields: Seq[ThriftStructField[T]] = structFields match {
    case Some(fields) => fields
    case None => PopulateMetaDataWithReflection.getFieldsWithReflection[T](codec, metaDataUtil)
  }

  /**
   * For non-unions, will return its [[ThriftStructFieldInfo ThriftStructFieldInfos]].
   *
   * For unions, will return an empty `Seq`.
   */
  val fieldInfos: Seq[ThriftStructFieldInfo] = structFieldInfos match {
    case Some(fieldInfos) => fieldInfos
    case None => PopulateMetaDataWithReflection.getFieldInfosWithReflection[T](codec, metaDataUtil)
  }

  /**
   * For unions, will return its [[ThriftUnionFieldInfo ThriftUnionFieldInfos]].
   *
   * For non-unions, will return an empty `Seq`.
   */
  val unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]] =
    unionFieldInfos match {
      case Some(unionFieldInfos) => unionFieldInfos
      case None =>
        PopulateMetaDataWithReflection.getUnionFieldsWithReflection[T](codec, metaDataUtil)
    }

  /**
   * Parsed annotations at the struct or union level. Left hand side of equal sign is the key,
   * right side is the value.
   */
  val structAnnotations: Map[String, String] = annotations match {
    case Some(structAnnotations) => structAnnotations
    case None =>
      PopulateMetaDataWithReflection.getStructAnnotationsWithReflection[T](codec, metaDataUtil)
  }
}

/**
 * Companion of ThriftStructField that provides helper methods.
 */
private object ThriftStructField {
  protected def snakeCaseToCamelCase(str: String): String = {
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
    thriftStructClass.getMethod(ThriftStructField.snakeCaseToCamelCase(tfield.name))

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
](val structFieldInfo: ThriftStructFieldInfo,
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
  val defaultValue: Option[Any]) {

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
  val thriftStructSubClassName: String = structCodecClass.getName.dropRight(1) // drop '$' from object name

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
        .filter { m =>
          m.getParameterTypes.length == 0 && m.getReturnType == classOf[TField]
        }
        .map { m =>
          val tfield = m.invoke(codec).asInstanceOf[TField]
          val manifest: scala.Option[Manifest[_]] = try {
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
