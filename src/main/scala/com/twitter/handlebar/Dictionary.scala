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

object Dictionary {
  sealed trait Value {
    def toBoolean: Boolean
    def toData: String
    def children: Seq[Dictionary]
  }

  case class StringValue(data: String) extends Value {
    def toBoolean = true
    def toData = data
    def children = Nil
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
  }

  case object NoValue extends Value {
    def toBoolean = false
    def toData = ""
    def children = Nil
  }

  implicit def v(data: String): Value = StringValue(data)
  implicit def v(data: Boolean): Value = BooleanValue(data)
  implicit def v(data: Seq[Dictionary]): Value = ListValue(data)
  implicit def v(data: Handlebar): Value = PartialValue(data)

  def apply(values: (String, Value)*) = {
    val dictionary = new Dictionary()
    dictionary.map ++= values.toMap mapValues {
      case ListValue(data) => ListValue(data map { _.copy(parent = Some(dictionary)) })
      case x => x
    }
    dictionary
  }
}

case class Dictionary private(
  private val parent: Option[Dictionary],
  private val map: mutable.Map[String, Dictionary.Value]
) {
  import Dictionary._

  override def toString = "Dictionary(parent=%s, map=%s)".format(parent.isDefined, map)

  def this() = this(None, new mutable.HashMap())

  def apply(key: String): Value = map.get(key).orElse {
    parent.map { _.apply(key) }
  }.getOrElse(NoValue)

  def update(key: String, data: String) {
    map(key) = StringValue(data)
  }

  def update(key: String, data: Boolean) {
    map(key) = BooleanValue(data)
  }

  def update(key: String, data: Seq[Dictionary]) {
    map(key) = ListValue(data map { _.copy(parent = Some(this)) })
  }

  def update(key: String, data: Handlebar) {
    map(key) = PartialValue(data)
  }
}
