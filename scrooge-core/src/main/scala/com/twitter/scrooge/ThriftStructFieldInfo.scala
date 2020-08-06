package com.twitter.scrooge

import org.apache.thrift.protocol.TField

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
