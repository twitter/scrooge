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

import Dictionary._
import org.specs.SpecificationWithJUnit

class DictionarySpec extends SpecificationWithJUnit {
  def v(data: Dictionary): Value = ListValue(Seq(data))
  def v(data: String): Value = CodeFragment(data)
  def v(data: Boolean): Value = BooleanValue(data)
  def v(data: Seq[Dictionary]): Value = ListValue(data)
  def v(data: Handlebar): Value = PartialValue(data)
  "Dictionary" should {
    "can be empty" in {
      val d = Dictionary()
      d("nothing") mustEqual Dictionary.NoValue
    }

    "stores" in {
      "boolean" in {
        val d = Dictionary("live" -> v(true), "banned" -> v(false))
        d("live").toBoolean must beTrue
        !d("banned").toBoolean must beTrue
        !d("not-here").toBoolean must beTrue
      }

      "string" in {
        val d = Dictionary("name" -> v("Commie"))
        d("name").toData mustEqual "Commie"
        d("name").toBoolean must beTrue
        d("not-here").toData mustEqual ""
      }

      "dictionaries" in {
        val stats = Seq(Dictionary("age" -> v("14")))
        val d = Dictionary("name" -> v("Commie"), "stats" -> v(stats))
        d("stats").children.size mustEqual 1
        d("nothing").children.size mustEqual 0
        d("stats").children.map { _("age").toData }.toList mustEqual List("14")
        d("stats").children.map { _("name").toData }.toList mustEqual List("Commie")
      }
    }
  }
}
