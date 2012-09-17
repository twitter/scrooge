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
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.twitter.scrooge.ParseException

@RunWith(classOf[JUnitRunner])
class ParserSpec extends FunSpec {
  describe("Parser") {
    it("all text") {
      assert(MustacheParser("hello\nthere") === Document(Seq(Data("hello\nthere"))))
    }

    it("interpolates") {
      val text = "say hello to {{friend}}, {{name}}"
      assert(MustacheParser(text) === Document(Seq(
        Data("say hello to "),
        Interpolation("friend"),
        Data(", "),
        Interpolation("name")
      )))
    }

    it("doesn't get confused by other {") {
      val text = "say { to {{friend}}, {{name}}"
      assert(MustacheParser(text) === Document(Seq(
        Data("say { to "),
        Interpolation("friend"),
        Data(", "),
        Interpolation("name")
      )))
    }

    it("errors on impossible ids") {
      val text = "hello {{"
      intercept[ParseException] { MustacheParser(text) }
    }

    it("section") {
      val text = "Classmates: {{#students}}Name: {{name}}{{/students}}"
      assert(MustacheParser(text) === Document(Seq(
        Data("Classmates: "),
        Section("students", Document(Seq(
          Data("Name: "),
          Interpolation("name")
        )), false)
      )))
    }

    it("nested section") {
      val text = "Planets: {{#planets}}{{name}} Moons: {{#moons}}{{name}}{{/moons}} :) {{/planets}}"
      assert(MustacheParser(text) === Document(Seq(
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
      intercept[ParseException] { MustacheParser(text) }
    }

    it("inverted section") {
      val text = "{{^space}}no space{{/space}}"
      assert(MustacheParser(text) === Document(Seq(
        Section("space", Document(Seq(
          Data("no space")
        )), true)
      )))
    }

    it("comments") {
      val text = "remember {{! these comments look stupid, like xml}} nothing."
      assert(MustacheParser(text) === Document(Seq(
        Data("remember "),
        Data(" nothing.")
      )))
    }

    it("partial") {
      val text = "{{#foo}}ok {{>other}}{{/foo}}"
      assert(MustacheParser(text) === Document(Seq(
        Section("foo", Document(Seq(
          Data("ok "),
          Partial("other")
        )), false)
      )))
    }

    it("triple braces is fine") {
      val text = "Hello, {{{foo}}}."
      assert(MustacheParser(text) === Document(Seq(
        Data("Hello, {"),
        Interpolation("foo"),
        Data("}.")
      )))
    }

    it("section with joiner") {
      val text = "Students: {{#students}}{{name}}{{/students|, }}"
      assert(MustacheParser(text) === Document(Seq(
        Data("Students: "),
        Section("students", Document(Seq(
          Interpolation("name")
        )), false, Some(", "))
      )))
    }
  }
}
