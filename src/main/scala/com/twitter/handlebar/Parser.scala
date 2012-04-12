/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.handlebar

import scala.collection.mutable
import scala.util.parsing.combinator._
import scala.util.parsing.combinator.lexical._
import scala.util.parsing.input.StreamReader
import java.io.StringReader

class ParseException(reason: String, cause: Throwable) extends Exception(reason, cause) {
  def this(reason: String) = this(reason, null)
}

object AST {
  case class Document(segments: Seq[Segment])
  sealed trait Segment
  case class Data(data: String) extends Segment
  case class Interpolation(name: String) extends Segment
  case class Section(name: String, document: Document, reversed: Boolean, joiner: Option[String] = None) extends Segment
  case class Partial(name: String) extends Segment
}

object Parser extends RegexParsers {
  import AST._

  override def skipWhitespace = false

  def document: Parser[Document] = rep(directive | data) ^^ { x => Document(x.flatten) }

  def directive: Parser[Option[Segment]] = interpolation | section | comment | partial

  def interpolation = "{{" ~> id <~ "}}" ^^ { x => Some(Interpolation(x)) }

  def startSection = "{{" ~> """#|\^""".r ~ id <~ "}}" ^^ { case prefix ~ id =>
    (prefix == "^", id)
  }

  def endSection = "{{/" ~> id ~ opt("|" ~> """([^}]|}(?!}))+""".r) <~ "}}"

  def section = startSection ~ document ~ endSection ^^ { case (reversed, id1) ~ doc ~ (id2 ~ joiner) =>
    if (id1 != id2) err("Expected " + id1 + ", got " + id2)
    Some(Section(id1, doc, reversed, joiner))
  }

  def comment = """\{\{!(.*?)}}""".r ^^^ None

  def partial = "{{>" ~> id <~ "}}" ^^ { x => Some(Partial(x)) }

  def data: Parser[Option[Segment]] = """([^{]+|\{(?!\{)|\{(?=\{\{))+""".r ^^ { x => Some(Data(x)) }

  def id = """[A-Za-z0-9_\.]+""".r

  // rawr.
  def apply(in: StreamReader): Document = {
    CleanupWhitespace {
      parseAll(document, in) match {
        case Success(result, _) => result
        case x @ Failure(msg, z) => throw new ParseException(x.toString)
        case x @ Error(msg, _) => throw new ParseException(x.toString)
      }
    }
  }
  def apply(in: String): Document = apply(StreamReader(new StringReader(in)))
}

/**
 * If a section header is on its own line, remove the trailing linefeed.
 */
object CleanupWhitespace extends (AST.Document => AST.Document) {
  import AST._

  def apply(document: Document): Document = {
    var afterSectionHeader = true
    val segments = document.segments.map {
      case Data(data) if afterSectionHeader && (data startsWith "\n") => {
        afterSectionHeader = false
        Data(data.substring(1))
      }
      case x @ Section(_, _, _, _) => {
        afterSectionHeader = true
        apply(x)
      }
      case x => {
        afterSectionHeader = false
        apply(x)
      }
    }
    Document(segments)
  }

  def apply(segment: Segment): Segment = {
    segment match {
      case Section(name, document, reversed, joiner) =>
        Section(name, apply(document), reversed, joiner)
      case x => x
    }
  }
}

