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

import org.scalatest.{AbstractSuite, Spec, Suite}
import org.scalatest.matchers.{Matcher, MatchResult, ShouldMatchers}

import AST._

class ParserSpec extends Spec {
  describe("Parser") {
    it("all text") {
      assert(Parser("hello\nthere") === Document(Seq(Data("hello\nthere"))))
    }

    it("interpolates") {
      val text = "say hello to {{friend}}, {{name}}"
      assert(Parser(text) === Document(Seq(
        Data("say hello to "),
        Interpolation("friend"),
        Data(", "),
        Interpolation("name")
      )))
    }

    it("doesn't get confused by other {") {
      val text = "say { to {{friend}}, {{name}}"
      assert(Parser(text) === Document(Seq(
        Data("say { to "),
        Interpolation("friend"),
        Data(", "),
        Interpolation("name")
      )))
    }

    it("errors on impossible ids") {
      val text = "hello {{"
      intercept[ParseException] { Parser(text) }
    }

    it("section") {
      val text = "Classmates: {{#students}}Name: {{name}}{{/students}}"
      assert(Parser(text) === Document(Seq(
        Data("Classmates: "),
        Section("students", Document(Seq(
          Data("Name: "),
          Interpolation("name")
        )), false)
      )))
    }

    it("nested section") {
      val text = "Planets: {{#planets}}{{name}} Moons: {{#moons}}{{name}}{{/moons}} :) {{/planets}}"
      assert(Parser(text) === Document(Seq(
        Data("Planets: "),
        Section("planets", Document(Seq(
          Interpolation("name"),
          Data(" Moons: "),
          Section("moons", Document(Seq(
            Interpolation("name")
          )), false),
          Data(" :) ")
        )), false)
      )))
    }

    it("complains about mismatched section headers") {
      val text = "Planets: {{#planets}}{{name}} Moons: {{#moons}}{{name}}{{/planets}}"
      intercept[ParseException] { Parser(text) }
    }

    it("inverted section") {
      val text = "{{^space}}no space{{/space}}"
      assert(Parser(text) === Document(Seq(
        Section("space", Document(Seq(
          Data("no space")
        )), true)
      )))
    }

    it("comments") {
      val text = "remember {{! these comments look stupid, like xml}} nothing."
      assert(Parser(text) === Document(Seq(
        Data("remember "),
        Data(" nothing.")
      )))
    }

    it("partial") {
      val text = "{{#foo}}ok {{>other}}{{/foo}}"
      assert(Parser(text) === Document(Seq(
        Section("foo", Document(Seq(
          Data("ok "),
          Partial("other")
        )), false)
      )))
    }

    it("triple braces is fine") {
      val text = "Hello, {{{foo}}}."
      assert(Parser(text) === Document(Seq(
        Data("Hello, {"),
        Interpolation("foo"),
        Data("}.")
      )))
    }
  }
}
