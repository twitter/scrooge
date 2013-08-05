package com.twitter.scrooge.ast

import scala.collection.mutable
import com.twitter.scrooge.frontend.ScroogeInternalException

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

  def toTitleCase(str: String): String = toCamelCase(str, true)

  /**
   * convert string to camel case, with the following fine print:
   *   - leading underscores are preserved
   *   - internal underscores are removed. Character following an underscore
   *     is converted to upper case.
   *   - first character (non underscore char) is upper case if
   *     firstCharUp is true, lower case if false
   *   - first character of the second and following parts (text between underscores)
   *     is always in upper case
   *   - if a part is all upper case it is converted to lower case (except for first character),
   *     in other cases case is preserved
   *
   *   Examples: (original, camel case, title case)
   *     (gen_html_report, genHtmlReport, GenHtmlReport)
   *     (GEN_HTML_REPORT, genHtmlReport, GenHtmlReport)
   *     (Gen_HTMLReport, genHTMLReport, GenHTMLReport)
   *     (Gen_HTML_Report, genHtmlReport, GenHtmlReport)
   *     (GENHTMLREPORT, genhtmlreport, Genhtmlreport)
   *     (genhtmlreport, genhtmlreport, Genhtmlreport)
   *     (genHtmlReport, genHtmlReport, GenHtmlReport)
   *     (genHTMLReport, genHTMLReport, GenHtmlReport)
   *     (_genHtmlReport, _genHtmlReport, _GenHtmlReport)
   */
  def toCamelCase(str: String, firstCharUp: Boolean = false): String = {
    str.takeWhile(_ == '_') + str.
      split('_').
      filterNot(_.isEmpty).
      zipWithIndex.map { case (part, ind) =>
        val first = if (ind == 0 && !firstCharUp) part(0).toLower else part(0).toUpper
        val isAllUpperCase = part.forall { c => c.isUpper || !c.isLetter }
        val rest = if (isAllUpperCase) part.drop(1).toLowerCase else part.drop(1)
        new mutable.StringBuilder(part.size).append(first).append(rest)
      }.
      mkString
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
