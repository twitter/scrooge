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

import scala.util.parsing.input.StreamReader
import java.io.StringReader

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

  def generate(template: String, dictionary: Dictionary): String =
    generate(Parser(StreamReader(new StringReader(template))), dictionary)

  def generate(template: StreamReader, dictionary: Dictionary): String =
    generate(Parser(template), dictionary)

  def generate(document: Document, dictionary: Dictionary): String = {
    document.segments.map { segment => process(segment, dictionary) }.mkString
  }

  private[this] def process(segment: Segment, dictionary: Dictionary): String = {
    segment match {
      case Data(data) => data
      case Interpolation(name) => dictionary(name).toData
      case Section(name, document, reversed, joiner) => {
        dictionary(name) match {
          case ListValue(items) => {
            if (reversed) "" else items.map { d => generate(document, d) }.mkString(joiner.getOrElse(""))
          }
          case other => {
            val expose = if (reversed) !other.toBoolean else other.toBoolean
            if (expose) generate(document, dictionary) else ""
          }
        }
      }
      case Partial(name) => {
        dictionary(name) match {
          case PartialValue(handlebar) => handlebar.generate(dictionary)
          case other => ""
        }
      }
    }
  }
}

case class Handlebar(document: AST.Document) {
  def this(template: StreamReader) = this(Parser(template))
  def this(template: String) = this(StreamReader(new StringReader(template)))

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
