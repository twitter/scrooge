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

package com.twitter.scrooge.mustache

object Handlebar {
  import MustacheAST._
  import Dictionary._

  private[this] val Space = " "
  private[this] val OnlySpaces = """^\s+$""".r

  def generate(template: String, dictionary: Dictionary): String =
    generate(0, MustacheParser(template), dictionary)

  def generate(document: Template, dictionary: Dictionary): String =
    generate(0, document, dictionary)

  private[this] def generate(indentLevel0: Int, document: Template, dictionary: Dictionary): String = {
    var indentLevel = indentLevel0
    document.segments.map { segment =>
      val (nextIndentLevel, processed) = process(indentLevel, segment, dictionary)
      indentLevel = indentLevel0 + nextIndentLevel
      processed
    }.mkString
  }

  private[this] def join(x: Seq[String], joiner: String): String = {
    if (x.size > 0 && x.head.endsWith("\n")) {
      // if each item is at least one entire line to itself, apply the joiner to the end of the
      // line, instead of onto its own line.
      val rv = x map { item =>
        if (item endsWith "\n") item.substring(0, item.size - 1) else item
      } mkString(joiner + "\n")
      rv + "\n"
    } else {
      x.mkString(joiner)
    }
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
              val contents = items.map { d => generate(indentLevel, document, d) }
              joiner map { join(contents, _) } getOrElse(contents.mkString)
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

case class Handlebar(document: MustacheAST.Template) {
  def this(template: String) = this(MustacheParser(template))

  /**
   * Create a string out of a template, using a dictionary to fill in the blanks.
   */
  def generate(dictionary: Dictionary) = Handlebar.generate(document, dictionary)

  /**
   * Given an `unpacker` function that can turn objects of type `T` into dictionaries, return a
   * new function of `T => String` that unpacks items of type `T` and runs them through the
   * template.
   */
  def generate[T](unpacker: T => Dictionary) = new (T => String) {
    def apply(item: T) = Handlebar.generate(document, unpacker(item))
  }

  def generate[T1, T2](unpacker: (T1, T2) => Dictionary) = new ((T1, T2) => String) {
    def apply(t1: T1, t2: T2) = Handlebar.generate(document, unpacker(t1, t2))
  }

  def generate[T1, T2, T3](unpacker: (T1, T2, T3) => Dictionary) = new ((T1, T2, T3) => String) {
    def apply(t1: T1, t2: T2, t3: T3) = Handlebar.generate(document, unpacker(t1, t2, t3))
  }
}

