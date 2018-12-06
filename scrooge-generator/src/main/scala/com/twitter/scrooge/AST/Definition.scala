package com.twitter.scrooge.ast

import com.twitter.scrooge.backend.ServiceOption

sealed abstract class Definition extends DefinitionNode {
  val sid: SimpleID
}

case class ConstDefinition(
  sid: SimpleID,
  fieldType: FieldType,
  value: RHS,
  docstring: Option[String])
    extends Definition

case class Typedef(
  sid: SimpleID,
  fieldType: FieldType,
  referentAnnotations: Map[String, String] = Map.empty,
  aliasAnnotations: Map[String, String] = Map.empty)
    extends Definition

case class Enum(
  sid: SimpleID,
  values: Seq[EnumField],
  docstring: Option[String],
  annotations: Map[String, String] = Map.empty)
    extends Definition

case class EnumField(
  sid: SimpleID,
  value: Int,
  docstring: Option[String],
  annotations: Map[String, String] = Map.empty)
    extends Definition

case class Senum(sid: SimpleID, values: Seq[String], annotations: Map[String, String] = Map.empty)
    extends Definition

sealed abstract class StructLike extends Definition {
  val originalName: String
  val fields: Seq[Field]
  val docstring: Option[String]
  val annotations: Map[String, String]
}

case class Struct(
  sid: SimpleID,
  originalName: String,
  fields: Seq[Field],
  docstring: Option[String],
  annotations: Map[String, String] = Map.empty)
    extends StructLike

case class Union(
  sid: SimpleID,
  originalName: String,
  fields: Seq[Field],
  docstring: Option[String],
  annotations: Map[String, String] = Map.empty)
    extends StructLike

case class FunctionArgs(sid: SimpleID, originalName: String, fields: Seq[Field])
    extends StructLike {
  override val docstring: Option[String] = None
  override val annotations: Map[String, String] = Map.empty
}

case class FunctionResult(
  sid: SimpleID,
  originalName: String,
  success: Option[Field], // None for void methods
  exceptions: Seq[Field])
    extends StructLike {
  override val fields = success.toList ++ exceptions
  override val docstring: Option[String] = None
  override val annotations: Map[String, String] = Map.empty
}

case class Exception_(
  sid: SimpleID,
  originalName: String,
  fields: Seq[Field],
  docstring: Option[String],
  annotations: Map[String, String] = Map.empty)
    extends StructLike

case class Service(
  sid: SimpleID,
  parent: Option[ServiceParent],
  functions: Seq[Function],
  docstring: Option[String],
  annotations: Map[String, String] = Map.empty,
  options: Set[ServiceOption] = Set.empty)
    extends Definition

/**
 * Identifier for the parent service.
 * @param filename Set if the parent service is imported from another file
 */
case class ServiceParent(sid: SimpleID, filename: Option[SimpleID])
