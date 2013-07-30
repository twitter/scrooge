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

import com.twitter.scrooge.mustache.Dictionary._
import org.specs.SpecificationWithJUnit

class HandlebarSpec extends SpecificationWithJUnit {
  def v(data: Dictionary): Value = ListValue(Seq(data))
  def v(data: String): Value = CodeFragment(data)
  def v(data: Boolean): Value = BooleanValue(data)
  def v(data: Seq[Dictionary]): Value = ListValue(data)
  def v(data: Handlebar): Value = PartialValue(data)
  "Handlebar" should {
    "without directives" in {
      val template = "there are no directives here"
      Handlebar.generate(template, Dictionary()) mustEqual template
    }

    "simple interpolation" in {
      val template = "Hello {{name}}!\nYou're looking {{how}} today."
      Handlebar.generate(template, Dictionary("name" -> v("Mary"), "how" -> v("sad"))) mustEqual
        "Hello Mary!\nYou're looking sad today."
    }

    "optional blocks" in {
      val template = "You {{#money}}have ${{money}}{{/money}}{{^money}}are broke{{/money}}."
      Handlebar.generate(template, Dictionary("money" -> v("5"))) mustEqual "You have $5."
      Handlebar.generate(template, Dictionary()) mustEqual "You are broke."
      Handlebar.generate(template, Dictionary("money" -> v(true))) mustEqual "You have $true."
      Handlebar.generate(template, Dictionary("money" -> v(false))) mustEqual "You are broke."
    }

    "iterates items" in {
      val cats = Seq(
        Dictionary("name" -> v("Commie")),
        Dictionary("name" -> v("Lola")),
        Dictionary("name" -> v("Lexi"))
      )

      "normally" in {
        val template = "The cats are named: {{#cats}}'{{name}}' {{/cats}}."
        Handlebar.generate(template, Dictionary("cats" -> v(cats))) mustEqual
          "The cats are named: 'Commie' 'Lola' 'Lexi' ."
      }

      "with a joiner" in {
        val template = "The cats are named: {{#cats}}{{name}}{{/cats|, }}."
        Handlebar.generate(template, Dictionary("cats" -> v(cats))) mustEqual
          "The cats are named: Commie, Lola, Lexi."
      }
    }

    "partial" in {
      val cities = Seq(
        Dictionary("city" -> v("New York"), "state" -> v("NY")),
        Dictionary("city" -> v("Atlanta"), "state" -> v("GA"))
      )
      val cityTemplate = new Handlebar("{{city}},\n{{state}}")
      val dictionary = Dictionary("cities" -> v(cities), "description" -> v(cityTemplate))

      "works" in {
        val template = "We have these cities:\n{{#cities}}\n{{>description}}\n{{/cities}}\n"
        Handlebar.generate(template, dictionary) mustEqual
          "We have these cities:\nNew York,\nNY\nAtlanta,\nGA\n"
      }

      "indents" in {
        val template = "We have these cities:\n{{#cities}}\n  {{>description}}\n{{/cities}}\n"
        Handlebar.generate(template, dictionary) mustEqual
          "We have these cities:\n  New York,\n  NY\n  Atlanta,\n  GA\n"
      }

      "indents nestedly" in {
        val template = "We have these cities:\n  {{>header}}\n"
        val headerTemplate = new Handlebar("Header:\n{{#cities}}\n  {{>description}}\n{{/cities}}\n")
        val dictionary = Dictionary(
          "cities" -> v(cities),
          "header" -> v(headerTemplate),
          "description" -> v(cityTemplate))
        Handlebar.generate(template, dictionary) mustEqual
          "We have these cities:\n  Header:\n    New York,\n    NY\n    Atlanta,\n    GA\n"
      }
    }

    "nests" in {
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
      rv mustEqual expect
    }
  }
}
