package com.twitter.scrooge.backend

import com.twitter.scrooge.ast.Document
import com.twitter.scrooge.frontend.{ResolvedDocument, TypeResolver}
import com.twitter.scrooge.mustache.HandlebarLoader
import com.twitter.scrooge.testutil.Spec

class TestGeneratorFactory extends GeneratorFactory {
  val language = "test"
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = new ScalaGenerator(
    doc,
    defaultNamespace,
    experimentFlags,
    new HandlebarLoader("/scalagen/", ".mustache")
  )
}

class GeneratorFactorySpec extends Spec {
  "GeneratorFactory" should {
    "be loadable" in {
      val generator = GeneratorFactory(
        "test",
        ResolvedDocument(new Document(Seq(), Seq()), TypeResolver()),
        "",
        Seq.empty[String]
      )

      generator.isInstanceOf[ScalaGenerator] must be(true)
    }
  }
}
