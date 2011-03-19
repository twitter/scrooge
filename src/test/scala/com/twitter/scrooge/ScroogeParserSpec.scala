package com.twitter.scrooge

import org.specs.Specification

class ScroogeParserSpec extends Specification {
  import AST._

  "ScroogeParser" should {
    val parser = new ScroogeParser()

    "constant" in {
      parser.parse("300", parser.constant) mustEqual IntConstant(300)
      parser.parse("300.5", parser.constant) mustEqual DoubleConstant(300.5)
      parser.parse("\"hello!\"", parser.constant) mustEqual StringConstant("hello!")
      parser.parse("'hello!'", parser.constant) mustEqual StringConstant("hello!")
      parser.parse("cat", parser.constant) mustEqual Identifier("cat")
      parser.parse("[ 4, 5 ]", parser.constant) mustEqual ListConstant(List(IntConstant(4),
        IntConstant(5)))
      parser.parse("{ 'name': 'Commie', 'home': 'San Francisco' }",
        parser.constant) mustEqual MapConstant(Map(StringConstant("name") -> StringConstant
        ("Commie"), StringConstant("home") -> StringConstant("San Francisco")))
    }
  }
}
