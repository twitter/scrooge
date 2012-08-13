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

import Dictionary._
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DictionarySpec extends FunSpec {
  describe("Dictionary") {
    it("can be empty") {
      val d = Dictionary()
      assert(d("nothing") === Dictionary.NoValue)
    }

    describe("stores") {
      it("boolean") {
        val d = Dictionary("live" -> v(true), "banned" -> v(false))
        assert(d("live").toBoolean)
        assert(!d("banned").toBoolean)
        assert(!d("not-here").toBoolean)
      }

      it("string") {
        val d = Dictionary("name" -> v("Commie"))
        assert(d("name").toData === "Commie")
        assert(d("name").toBoolean)
        assert(d("not-here").toData === "")
      }

      it("dictionaries") {
        val stats = Seq(Dictionary("age" -> v("14")))
        val d = Dictionary("name" -> v("Commie"), "stats" -> v(stats))
        assert(d("stats").children.size === 1)
        assert(d("nothing").children.size === 0)
        assert(d("stats").children.map { _("age").toData }.toList === List("14"))
        assert(d("stats").children.map { _("name").toData }.toList === List("Commie"))
      }
    }
  }
}
