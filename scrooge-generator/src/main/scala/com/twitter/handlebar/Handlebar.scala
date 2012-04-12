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

case class Unpacker[T](
  document: AST.Document,
  unpacker: T => Dictionary,
  handlebar: Handlebar
) extends (T => String) {
  def apply(item: T) = Handlebar.generate(document, unpacker(item))
}

object Handlebar {
  import AST._
  import Dictionary._

  private[this] val Space = " "
  private[this] val OnlySpaces = """^\s+$""".r

  def generate(template: String, dictionary: Dictionary): String =
    generate(0, Parser(template), dictionary)

  def generate(document: Document, dictionary: Dictionary): String =
    generate(0, document, dictionary)

  private[this] def generate(indentLevel0: Int, document: Document, dictionary: Dictionary): String = {
    var indentLevel = indentLevel0
    document.segments.map { segment =>
      val (nextIndentLevel, processed) = process(indentLevel, segment, dictionary)
      indentLevel = indentLevel0 + nextIndentLevel
      processed
    }.mkString
  }

  private[this] def process(indentLevel: Int, segment: Segment, dictionary: Dictionary): (Int, String) = {
    var nextIndentLevel = 0
    val processed = segment match {
      case Data(data) => {
        nextIndentLevel = {
          val lastLine = data.split("\n").lastOption.getOrElse("")
          if (OnlySpaces.findFirstIn(lastLine).isDefined) {
            lastLine.length
          } else {
            0
          }
        }
        data
      }
      case Interpolation(name) => dictionary(name).toData
      case Section(name, document, reversed, joiner) => {
        dictionary(name) match {
          case ListValue(items) => {
            if (reversed) {
              ""
            } else {
              items.map { d => generate(indentLevel, document, d) }.mkString(joiner.getOrElse(""))
            }
          }
          case other => {
            val expose = if (reversed) !other.toBoolean else other.toBoolean
            if (expose) {
              generate(indentLevel, document, dictionary)
            } else {
              ""
            }
          }
        }
      }
      case Partial(name) => {
        dictionary(name) match {
          case PartialValue(handlebar) => {
            val generated = handlebar.generate(dictionary)
            if (indentLevel > 0) {
              val indentation = Space * indentLevel
              val indented =
                generated.split("\n").zipWithIndex.map { case (line, i) =>
                  if (i > 0 && !brittspace(line)) {
                    indentation + line
                  } else {
                    line
                  }
                } mkString("\n")
              indented
            } else
              generated
          }
          case other => ""
        }
      }
    }
    (nextIndentLevel, processed)
  }

  private[this] def brittspace(line: String) = OnlySpaces.findFirstIn(line).isDefined
}

case class Handlebar(document: AST.Document) {
  def this(template: String) = this(Parser(template))

  /**
   * Create a string out of a template, using a dictionary to fill in the blanks.
   */
  def generate(dictionary: Dictionary) = Handlebar.generate(document, dictionary)

  /**
   * Given an `unpacker` function that can turn objects of type `T` into dictionaries, return a
   * new function of `T => String` that unpacks items of type `T` and runs them through the
   * template.
   */
  def generate[T](unpacker: T => Dictionary) = new Unpacker[T](document, unpacker, this)
}
