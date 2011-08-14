package com.twitter.scrooge

import org.specs.Specification

object ASTSpec extends Specification {
  import AST._

  "camelCase" should {
    val cases = List(
      "hello" -> "hello",
      "hello_world" -> "helloWorld",
      "a_b_c_d" -> "aBCD"
    )
    cases foreach {
      case (input, expected) =>
        input in {
          camelCase(input) mustEqual expected
        }
    }
  }

  "Document" should {
    "generate correct scalaNamespace from java" in {
      val doc = Document(Seq(Namespace("java", "com.twitter.oatmeal")), Nil)
      doc.scalaNamespace mustEqual "com.twitter.oatmeal"
    }

    "generate correct scalaNamespace from scala" in {
      val doc = Document(Seq(Namespace("scala", "com.twitter.oatmeal")), Nil)
      doc.scalaNamespace mustEqual "com.twitter.oatmeal"
    }

    "generate default scalaNamespace" in {
      val doc = Document(Nil, Nil)
      doc.scalaNamespace mustEqual "thrift"
    }
  }
}
