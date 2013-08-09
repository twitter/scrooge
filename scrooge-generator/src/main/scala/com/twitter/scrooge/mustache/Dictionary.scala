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

import scala.collection.mutable

object Dictionary {
  sealed trait Value {
    def toBoolean: Boolean
    def toData: String
    def children: Seq[Dictionary]
  }

  case class CodeFragment(data: String) extends Value {
    def toBoolean = data != ""
    def toData = data
    def children = Nil
    override def toString = toData
    def append(suffix: String): CodeFragment = CodeFragment(data + suffix)
  }

  case class BooleanValue(data: Boolean) extends Value {
    def toBoolean = data
    def toData = if (data) "true" else "false"
    def children = Nil
  }

  case class ListValue(data: Seq[Dictionary]) extends Value {
    def toBoolean = true
    def toData = "?"
    def children = data
  }

  case class PartialValue(partial: Handlebar) extends Value {
    def toBoolean = true
    def toData = "?"
    def children = Nil
    override def toString = "<partial>"
  }

  case object NoValue extends Value {
    def toBoolean = false
    def toData = ""
    def children = Nil
  }

  /**
   * Wrap generated code fragments in the form of Strings in a dictionary value.
   */
  def codify(code: String): CodeFragment = CodeFragment(code)
  /**
   * Wrap a boolean flag in a dictionary value.
   */
  def v(data: Boolean): Value =  BooleanValue(data)

  /**
   * Add a child Dictionary. This is used to process Sections in mustache templates
   */
  def v(data: Dictionary): Value = ListValue(Seq(data))

  /**
   * Add children dictionaries. This is used to process Sections in mustache templates
   */
  def v(data: Seq[Dictionary]): Value = ListValue(data)

  /**
   * Unwraps the given value, if any, or returns NoValue.
   */
  def v(data: Option[Value]): Value = data.getOrElse(NoValue)

  /**
   * Wrap a handle bar in dictionary value. This is used to process Partial in mustache templates
   */
  def v(data: Handlebar): Value = PartialValue(data)

  def apply(values: (String, Value)*) = new Dictionary ++= (values: _*)
}

case class Dictionary private(
  private val parent: Option[Dictionary],
  private val map: mutable.Map[String, Dictionary.Value]
) {
  import Dictionary._

  override def toString = "Dictionary(parent=%s, map=%s)".format(parent.isDefined, map)

  def this() = this(None, new mutable.HashMap())

  def apply(key: String): Value = {
    map.get(key).map {
      case ListValue(data) => ListValue(data map { _.copy(parent = Some(this)) })
      case v => v
    }.orElse {
      parent.map { _.apply(key) }
    }.getOrElse(NoValue)
  }

  def update(key: String, data: String) {
    map(key) = CodeFragment(data)
  }

  def update(key: String, data: Boolean) {
    map(key) = BooleanValue(data)
  }

  def update(key: String, data: Seq[Dictionary]) {
    map(key) = ListValue(data)
  }

  def update(key: String, data: Handlebar) {
    map(key) = PartialValue(data)
  }

  def ++=(values: (String, Value)*) = {
    map ++= values.toMap
    this
  }

  def +(dict: Dictionary) = {
    new Dictionary() ++= (this.map.toSeq: _*) ++= (dict.map.toSeq: _*)
  }
}
