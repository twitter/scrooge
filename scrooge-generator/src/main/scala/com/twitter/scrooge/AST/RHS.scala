package com.twitter.scrooge.ast

sealed abstract class RHS extends ValueNode
sealed abstract class Literal extends RHS
case class BoolLiteral(value: Boolean) extends Literal
case class IntLiteral(value: Long) extends Literal
case class DoubleLiteral(value: Double) extends Literal
case class StringLiteral(value: String) extends Literal
case object NullLiteral extends Literal

case class ListRHS(elems: Seq[RHS]) extends RHS
case class SetRHS(elems: Set[RHS]) extends RHS
case class MapRHS(elems: Seq[(RHS, RHS)]) extends RHS
case class StructRHS(elems: Map[SimpleID, RHS]) extends RHS
case class EnumRHS(enum: Enum, value: EnumField) extends RHS
case class IdRHS(id: Identifier) extends RHS