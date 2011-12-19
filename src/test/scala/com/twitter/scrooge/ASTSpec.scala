package com.twitter.scrooge

import org.specs.Specification

object ASTSpec extends Specification {
  import AST._

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

    "map namespaces" in {
      val javaOatmealNs = Namespace("java", "com.twitter.oatmeal")
      val javaGranolaNs = Namespace("java", "com.twitter.granola")
      val rbOatmealNs = Namespace("rb", "Oatmeal")
      val doc = Document(Seq(javaOatmealNs, rbOatmealNs), Nil)
      val namespaceMap = Map(javaOatmealNs.name -> javaGranolaNs.name)
      doc.mapNamespaces(namespaceMap) mustEqual
        Document(Seq(javaGranolaNs, rbOatmealNs), Nil)
    }

    "map namespaces recursively" in {
      val javaOatmealNs = Namespace("java", "com.twitter.oatmeal")
      val javaGranolaNs = Namespace("java", "com.twitter.granola")
      val doc1 = Document(Seq(javaOatmealNs), Nil)
      val doc2 = Document(Seq(javaOatmealNs, Include("other", doc1)), Nil)
      val namespaceMap = Map(javaOatmealNs.name -> javaGranolaNs.name)
      doc2.mapNamespaces(namespaceMap) must beLike {
        case Document(Seq(`javaGranolaNs`, Include(_, included)), Nil) =>
          included mustEqual Document(Seq(javaGranolaNs), Nil)
          true
      }
    }
  }

  "Identifier" should {
    "camelize to title-case" in {
      Identifier("HELLO_WORLD").camelize mustEqual Identifier("HelloWorld")
    }
  }

  "EnumValue" should {
    "camelize to title-case" in {
      EnumValue("HELLO_WORLD", 3).camelize mustEqual EnumValue("HelloWorld", 3)
    }
  }

  "Enum" should {
    "camelize values" in {
      Enum("Greeting", Seq(EnumValue("HELLO_WORLD", 3))).camelize mustEqual
        Enum("Greeting", Seq(EnumValue("HelloWorld", 3)))
    }
  }
}
