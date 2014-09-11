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

import MustacheAST._
import com.twitter.scrooge.frontend.ParseException
import com.twitter.scrooge.testutil.Spec

class ParserSpec extends Spec {
  "Parser" should {
    "all text" in {
      MustacheParser("hello\nthere") must be(Template(Seq(Data("hello\nthere"))))
    }

    "interpolates" in {
      val text = "say hello to {{friend}}, {{name}}"
      MustacheParser(text) must be(Template(Seq(
        Data("say hello to "),
        Interpolation("friend"),
        Data(", "),
        Interpolation("name")
      )))
    }

    "doesn't get confused by other {" in {
      val text = "say { to {{friend}}, {{name}}"
      MustacheParser(text) must be(Template(Seq(
        Data("say { to "),
        Interpolation("friend"),
        Data(", "),
        Interpolation("name")
      )))
    }

    "errors on impossible ids" in {
      val text = "hello {{"
      intercept[ParseException] {
        MustacheParser(text)
      }
    }

    "section" in {
      val text = "Classmates: {{#students}}Name: {{name}}{{/students}}"
      MustacheParser(text) must be(Template(Seq(
        Data("Classmates: "),
        Section("students", Template(Seq(
          Data("Name: "),
          Interpolation("name")
        )), false)
      )))
    }

    "nested section" in {
      val text = "Planets: {{#planets}}{{name}} Moons: {{#moons}}{{name}}{{/moons}} :) {{/planets}}"
      MustacheParser(text) must be(Template(Seq(
        Data("Planets: "),
        Section("planets", Template(Seq(
          Interpolation("name"),
          Data(" Moons: "),
          Section("moons", Template(Seq(
            Interpolation("name")
          )), false),
          Data(" :) ")
        )), false)
      )))
    }

    "complains about mismatched section headers" in {
      val text = "Planets: {{#planets}}{{name}} Moons: {{#moons}}{{name}}{{/planets}}"
      intercept[ParseException] {
        MustacheParser(text)
      }
    }

    "inverted section" in {
      val text = "{{^space}}no space{{/space}}"
      MustacheParser(text) must be(Template(Seq(
        Section("space", Template(Seq(
          Data("no space")
        )), true)
      )))
    }

    "comments" in {
      val text = "remember {{! these comments look stupid, like xml}} nothing."
      MustacheParser(text) must be(Template(Seq(
        Data("remember "),
        Data(" nothing.")
      )))
    }

    "partial" in {
      val text = "{{#foo}}ok {{>other}}{{/foo}}"
      MustacheParser(text) must be(Template(Seq(
        Section("foo", Template(Seq(
          Data("ok "),
          Partial("other")
        )), false)
      )))
    }

    "triple braces is fine" in {
      val text = "Hello, {{{foo}}}."
      MustacheParser(text) must be(Template(Seq(
        Data("Hello, {"),
        Interpolation("foo"),
        Data("}.")
      )))
    }

    "section with joiner" in {
      val text = "Students: {{#students}}{{name}}{{/students|, }}"
      MustacheParser(text) must be(Template(Seq(
        Data("Students: "),
        Section("students", Template(Seq(
          Interpolation("name")
        )), false, Some(", "))
      )))
    }
  }
}
