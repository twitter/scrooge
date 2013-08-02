package com.twitter.scrooge.ast

import org.specs.SpecificationWithJUnit

class ASTSpec extends SpecificationWithJUnit {
  "Namespace" should {
    "generate correct namespace from java" in {
      val doc = Document(Seq(Namespace("java", Identifier("com.twitter.oatmeal"))), Nil)
      doc.namespace("java").isDefined must beTrue
      doc.namespace("java").get.fullName mustEqual ("com.twitter.oatmeal")
    }

    "reject undefined namespace" in {
      val doc = Document(Seq(Namespace("warble", Identifier("com.twitter.oatmeal"))), Nil)
      doc.namespace("garble") mustEqual None
    }

    "map namespaces" in {
      val javaOatmealNs = Namespace("java", Identifier("com.twitter.oatmeal"))
      val javaGranolaNs = Namespace("java", Identifier("com.twitter.granola"))
      val rbOatmealNs = Namespace("rb", Identifier("Oatmeal"))
      val doc = Document(Seq(javaOatmealNs, rbOatmealNs), Nil)
      val namespaceMap = Map(javaOatmealNs.id.fullName -> javaGranolaNs.id.fullName)
      doc.mapNamespaces(namespaceMap) mustEqual
        Document(Seq(javaGranolaNs, rbOatmealNs), Nil)
    }

    "map namespaces recursively" in {
      val javaOatmealNs = Namespace("java", Identifier("com.twitter.oatmeal"))
      val javaGranolaNs = Namespace("java", Identifier("com.twitter.granola"))
      val doc1 = Document(Seq(javaOatmealNs), Nil)
      val doc2 = Document(Seq(javaOatmealNs, Include("other", doc1)), Nil)
      val namespaceMap = Map(javaOatmealNs.id.fullName -> javaGranolaNs.id.fullName)
      doc2.mapNamespaces(namespaceMap) must beLike {
        case Document(Seq(javaGranolaNs, Include(_, included)), Nil) =>
          included mustEqual Document(Seq(javaGranolaNs), Nil)
          true
      }
    }
  }

  "Identifier" should {
    val simpleCases = List(
      "hello" -> ("hello", "Hello"),
      "hello_world" -> ("helloWorld", "HelloWorld"),
      "a_b_c_d" -> ("aBCD", "ABCD"),
      "HELLO_WORLD" -> ("helloWorld", "HelloWorld"),
      "helloWorld" -> ("helloWorld", "HelloWorld"),
      "hello_World" -> ("helloWorld", "HelloWorld"),
      "HELLOWORLD" -> ("helloworld", "Helloworld"),
      "_Foo_bar" -> ("_fooBar", "_FooBar"),
      "__foo_bar" -> ("__fooBar", "__FooBar"),
      "ThriftClientRequestID" -> ("thriftClientRequestID", "ThriftClientRequestID"),
      "TChatbirdKey" -> ("tChatbirdKey", "TChatbirdKey"),
      "gen_html_report" -> ("genHtmlReport", "GenHtmlReport"),
      "GEN_HTML_REPORT" -> ("genHtmlReport", "GenHtmlReport"),
      "Gen_HTMLReport" -> ("genHTMLReport", "GenHTMLReport"),
      "Gen_HTML_Report" -> ("genHtmlReport", "GenHtmlReport"),
      "GENHTMLREPORT" -> ("genhtmlreport", "Genhtmlreport"),
      "genHTMLReport" -> ("genHTMLReport", "GenHTMLReport"),
      "genhtmlreport" -> ("genhtmlreport", "Genhtmlreport"),
      "genHtmlReport" -> ("genHtmlReport", "GenHtmlReport"),
      "_genHtmlReport" -> ("_genHtmlReport", "_GenHtmlReport"),
      "__genHtmlReport" -> ("__genHtmlReport", "__GenHtmlReport"),
      "WITH_NUMBER0" -> ("withNumber0", "WithNumber0")
    )
    "camel case conversion" in {
      simpleCases foreach {
        case (input, (expected, _)) =>
          val sid = SimpleID(input)
          sid.toCamelCase.name mustEqual expected
      }
    }

    "title case conversion" in {
      simpleCases foreach {
        case (input, (_, expected)) =>
          val sid = SimpleID(input)
          sid.toTitleCase.name mustEqual expected
      }
    }
  }
}
