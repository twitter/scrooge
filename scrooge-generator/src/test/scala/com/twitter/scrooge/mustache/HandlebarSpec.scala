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

import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class HandlebarSpec extends FunSpec {
  import Dictionary._

  describe("Handlebar") {
    it("without directives") {
      val template = "there are no directives here"
      assert(Handlebar.generate(template, Dictionary()) === template)
    }

    it("simple interpolation") {
      val template = "Hello {{name}}!\nYou're looking {{how}} today."
      assert(Handlebar.generate(template, Dictionary("name" -> v("Mary"), "how" -> v("sad"))) ===
        "Hello Mary!\nYou're looking sad today.")
    }

    it("optional blocks") {
      val template = "You {{#money}}have ${{money}}{{/money}}{{^money}}are broke{{/money}}."
      assert(Handlebar.generate(template, Dictionary("money" -> v("5"))) === "You have $5.")
      assert(Handlebar.generate(template, Dictionary()) === "You are broke.")
      assert(Handlebar.generate(template, Dictionary("money" -> v(true))) === "You have $true.")
      assert(Handlebar.generate(template, Dictionary("money" -> v(false))) === "You are broke.")
    }

    describe("iterates items") {
      val cats = Seq(
        Dictionary("name" -> "Commie"),
        Dictionary("name" -> "Lola"),
        Dictionary("name" -> "Lexi")
      )

      it("normally") {
        val template = "The cats are named: {{#cats}}'{{name}}' {{/cats}}."
        assert(Handlebar.generate(template, Dictionary("cats" -> v(cats))) ===
          "The cats are named: 'Commie' 'Lola' 'Lexi' .")
      }

      it("with a joiner") {
        val template = "The cats are named: {{#cats}}{{name}}{{/cats|, }}."
        assert(Handlebar.generate(template, Dictionary("cats" -> v(cats))) ===
          "The cats are named: Commie, Lola, Lexi.")
      }
    }

    describe("partial") {
      val cities = Seq(
        Dictionary("city" -> v("New York"), "state" -> v("NY")),
        Dictionary("city" -> v("Atlanta"), "state" -> v("GA"))
      )
      val cityTemplate = new Handlebar("{{city}},\n{{state}}")
      val dictionary = Dictionary("cities" -> v(cities), "description" -> v(cityTemplate))

      it("works") {
        val template = "We have these cities:\n{{#cities}}\n{{>description}}\n{{/cities}}\n"
        assert(Handlebar.generate(template, dictionary) ===
          "We have these cities:\nNew York,\nNY\nAtlanta,\nGA\n")
      }

      it("indents") {
        val template = "We have these cities:\n{{#cities}}\n  {{>description}}\n{{/cities}}\n"
        assert(Handlebar.generate(template, dictionary) ===
          "We have these cities:\n  New York,\n  NY\n  Atlanta,\n  GA\n")
      }

      it("indents nestedly") {
        val template = "We have these cities:\n  {{>header}}\n"
        val headerTemplate = new Handlebar("Header:\n{{#cities}}\n  {{>description}}\n{{/cities}}\n")
        val dictionary = Dictionary(
          "cities" -> v(cities),
          "header" -> v(headerTemplate),
          "description" -> v(cityTemplate))
        assert(Handlebar.generate(template, dictionary) ===
          "We have these cities:\n  Header:\n    New York,\n    NY\n    Atlanta,\n    GA\n")
      }
    }

    it("nests") {
      val students = Seq(
        Dictionary(
          "cats" -> v(
            Dictionary(
              "info" -> v(Seq(
                Dictionary("city" -> v("Anchorage")),
                Dictionary("city" -> v("Seattle")),
                Dictionary("city" -> v("Lihu'a")))))),
          "name" -> v("Taro")), // NOTE: "name" must be AFTER "cats->info" to exhibit nesting bug
        Dictionary(
          "cats" -> v(
            Dictionary(
              "info" -> v(Seq(
                Dictionary("city" -> v("Chicago")),
                Dictionary("city" -> v("Knoxville")))))),
          "name" -> v("Mira")))
      // this also (accidentally) tests whitespace cleanup.
      val template = """My book report:
{{#students}}
One of our students is {{name}}.
{{#cats}}
{{#info}}
{{name}} has a cat that lives in {{city}}.
{{/info}}
{{/cats}}
{{/students}}"""
      val rv = Handlebar.generate(template, Dictionary("students" -> v(students)))
      val expect = """My book report:
One of our students is Taro.
Taro has a cat that lives in Anchorage.
Taro has a cat that lives in Seattle.
Taro has a cat that lives in Lihu'a.
One of our students is Mira.
Mira has a cat that lives in Chicago.
Mira has a cat that lives in Knoxville.
"""
      assert(rv === expect)
    }
  }
}
