package com.twitter.scrooge

import java.lang.reflect.Method
import org.apache.thrift.protocol.{TField, TProtocol}
import scala.collection.mutable.StringBuilder

/**
 * A simple class for generic introspection on ThriftStruct classes.
 */
final class ThriftStructMetaData[T <: ThriftStruct](val codec: ThriftStructCodec[T]) {
  private[this] def toCamelCase(str: String): String = {
    str.takeWhile(_ == '_') + str.
      split('_').
      filterNot(_.isEmpty).
      zipWithIndex.map { case (part, ind) =>
        val first = if (ind == 0) part(0).toLower else part(0).toUpper
        val isAllUpperCase = part.forall(_.isUpper)
        val rest = if (isAllUpperCase) part.drop(1).toLowerCase else part.drop(1)
        new StringBuilder(part.size).append(first).append(rest)
      }.
      mkString
  }

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  val codecClass = codec.getClass

  /**
   * The fully qualified name of the ThriftStruct sublcass.
   */
  val structClassName = codecClass.getName.dropRight(1) // drop '$' from object name

  /**
   * Gets the unqualified name of the struct.
   */
  val structName = structClassName.split("\\.").last

  /**
   * The Class object for ThriftStruct subclass.
   */
  val structClass = codecClass.getClassLoader.loadClass(structClassName).asInstanceOf[Class[T]]

  /**
   * A Seq of ThriftStructFields representing the fields defined in the ThriftStruct.
   */
  val fields: Seq[ThriftStructField[T]] =
    codecClass.getMethods.toList filter { m =>
      m.getParameterTypes.size == 0 && m.getReturnType == classOf[TField]
    } map { m =>
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

final class ThriftStructField[T <: ThriftStruct](val tfield: TField, val method: Method, val manifest: scala.Option[Manifest[_]]) {
  /**
   * The TField field name, same as the method name on the ThriftStruct for the value.
   */
  def name = tfield.name

  /**
   * The TField field id, as defined in the source thrift file.
   */
  def id = tfield.id

  /**
   * The TField field type.  See TType for possible values.
   */
  def `type` = tfield.`type`

  /**
   * Gets the value of the field from the struct. You can specify the expected return
   * type, rather than casting explicitly.
   */
  def getValue[R](struct: T): R = method.invoke(struct).asInstanceOf[R]
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

