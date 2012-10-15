package com.twitter.scrooge.ast

import collection.mutable.StringBuilder
import com.twitter.scrooge.ScroogeInternalException

sealed abstract class Identifier extends IdNode {
  // It was intentional not to override toString. Instead, use
  // "fullName" to indicate its purpose.
  def fullName: String

  def toCamelCase: Identifier
  def toTitleCase: Identifier
  def toUpperCase: Identifier
  def toLowerCase: Identifier

  // to prevent accidental use of Identifier as String
  private[scrooge] def +(str: String): String =
    throw new ScroogeInternalException("do not use \"+\" operation on Identifiers")
}

object Identifier {
  // constructor
  def apply(str: String): Identifier = {
    assert(!str.isEmpty)
    val ids = str.split("\\.")
    if (ids.size == 1)
      SimpleID(ids.head)
    else
      QualifiedID(ids)
  }

  def toCamelCase(str: String): String = toCamelCase(str, false)

  def toTitleCase(str: String): String = toCamelCase(str, true)

  object State extends Enumeration {
    type State = Value
    val NextUp, NextDown, Lower, Upper, LeadIn = Value
  }

  // case conversion
  private[this] def toCamelCase(str: String, firstCharUp: Boolean): String = {
    import State._

    var state = LeadIn
    val sb = new StringBuilder(str.length)

    // c should be upper only if following _ or following <lower> and is <upper>
    // leading underscores should be preserved
    for (c <- str) {
      if (c == '_') {
        state match {
          case LeadIn => sb.append('_')
          case _ => state = NextUp
        }
      } else {
        state match {
          case LeadIn => sb.append(if (firstCharUp) c.toUpper else c.toLower)
          case NextUp => sb.append(c.toUpper)
          case NextDown => sb.append(c.toLower)
          case Lower => sb.append(c)
          case Upper => sb.append(c.toLower)
        }
        state = if (c.isUpper) Upper else Lower
      }
    }
    sb.toString
  }
}

case class SimpleID(name: String) extends Identifier {
  assert(!name.contains(".") && !name.isEmpty) // name is a simple string
  val fullName: String = name

  def toCamelCase = SimpleID(Identifier.toCamelCase(name))
  def toTitleCase = SimpleID(Identifier.toTitleCase(name))
  def toUpperCase = SimpleID(name.toUpperCase)
  def toLowerCase = SimpleID(name.toLowerCase)

  // append and prepend only available for SimpleID
  // To encourage correct usage of SimpleID, we intentionally don't use implicit
  // string conversions
  def append(other: String): SimpleID = {
    assert(!other.isEmpty && !other.contains("."))
    SimpleID(name + other)
  }

  def prepend(other: String): SimpleID = {
    assert(!other.isEmpty && !other.contains("."))
    SimpleID(other + name)
  }

  def addScope(scope: Identifier): QualifiedID =
    QualifiedID(scope match {
      case SimpleID(s) => Seq(s, this.name)
      case QualifiedID(names) => names :+ name
    })
}

case class QualifiedID(names: Seq[String]) extends Identifier {
  assert(names.size >= 2) // at least a scope and a name
  assert(!names.exists(_.isEmpty))
  val fullName: String = names.mkString(".")

  // case conversion only happens on the last id
  def toCamelCase =
    QualifiedID(names.dropRight(1) :+ Identifier.toCamelCase(names.last))
  def toTitleCase =
    QualifiedID(names.dropRight(1) :+ Identifier.toTitleCase(names.last))
  def toUpperCase =
    QualifiedID(names.dropRight(1) :+ names.last.toUpperCase)
  def toLowerCase =
    QualifiedID(names.dropRight(1) :+ names.last.toLowerCase)

  def head: SimpleID = SimpleID(names.head)
  def tail: Identifier = Identifier(names.tail.mkString("."))

  def qualifier: Identifier = Identifier(names.dropRight(1).mkString("."))
  def name: SimpleID = SimpleID(names.last)
}
