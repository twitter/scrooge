package com.twitter.scrooge.ast


sealed abstract class Definition extends DefinitionNode {
  val sid: SimpleID
}

case class ConstDefinition(
  sid: SimpleID,
  fieldType: FieldType,
  value: RHS,
  docstring: Option[String]
) extends Definition

case class Typedef(sid: SimpleID, fieldType: FieldType) extends Definition

case class Enum(
  sid: SimpleID,
  values: Seq[EnumField],
  docstring: Option[String]
) extends Definition

case class EnumField(sid: SimpleID, value: Int) extends Definition
case class Senum(sid: SimpleID, values: Seq[String]) extends Definition

sealed abstract class StructLike extends Definition {
  val fields: Seq[Field]
  val docstring: Option[String]
}

case class Struct(
  sid: SimpleID,
  fields: Seq[Field],
  docstring: Option[String]
) extends StructLike

case class FunctionArgs(sid: SimpleID, fields: Seq[Field]) extends StructLike {
  override val docstring: Option[String] = None
}
case class FunctionResult(sid: SimpleID, fields: Seq[Field]) extends StructLike {
  override val docstring: Option[String] = None
}

case class Exception_(
  sid: SimpleID,
  fields: Seq[Field],
  docstring: Option[String]
) extends StructLike


case class Service(
  sid: SimpleID,
  parent: Option[ServiceParent],
  functions: Seq[Function],
  docstring: Option[String]
) extends Definition

object ServiceParent {
  def apply(service: Service): ServiceParent = ServiceParent(service.sid.name, Some(service))
}

case class ServiceParent(name: String, service: Option[Service] = None)
