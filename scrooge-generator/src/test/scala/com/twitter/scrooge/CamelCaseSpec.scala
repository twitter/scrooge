package com.twitter.scrooge

import org.specs.SpecificationWithJUnit

class CamelCaseSpec extends SpecificationWithJUnit {
  import AST._

  val cases = List(
    "hello" -> ("hello", "Hello"),
    "hello_world" -> ("helloWorld", "HelloWorld"),
    "a_b_c_d" -> ("aBCD", "ABCD"),
    "HELLO_WORLD" -> ("helloWorld", "HelloWorld"),
    "helloWorld" -> ("helloWorld", "HelloWorld"),
    "hello_World" -> ("helloWorld", "HelloWorld"),
    "HELLOWORLD" -> ("helloworld", "Helloworld")
  )

  "CamelCase" should {
    cases foreach {
      case (input, (expected, _)) =>
        (input + " -> " + expected) in {
          CamelCase(input) mustEqual expected
        }
    }
  }

  "TitleCase" should {
    cases foreach {
      case (input, (_, expected)) =>
        (input + " -> " + expected) in {
          TitleCase(input) mustEqual expected
        }
    }
  }
}
