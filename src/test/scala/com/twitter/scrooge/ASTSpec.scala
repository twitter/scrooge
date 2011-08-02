package com.twitter.scrooge

import org.specs.Specification

object ASTSpec extends Specification {
  "camelCase" should {
    val cases = List(
      "hello" -> "hello",
      "hello_world" -> "helloWorld",
      "a_b_c_d" -> "aBCD"
    )
    cases foreach {
      case (input, expected) =>
        input in {
          AST.camelCase(input) mustEqual expected
        }
    }
  }
}
