package com.twitter.scrooge.backend

import com.twitter.scrooge.frontend.ResolvedDocument
import com.twitter.scrooge.testutil.Spec

class TestGeneratorFactory extends GeneratorFactory {
  val lang = "test"
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator = new ScalaGenerator(includeMap, defaultNamespace, experimentFlags)
}

class GeneratorFactorySpec extends Spec {
  "GeneratorFactory" should {
    "be loadable" in {
      val generator = Generator("test", Map.empty[String, ResolvedDocument], "", Seq.empty[String])
      generator.isInstanceOf[ScalaGenerator] must be(true)
    }
  }
}
