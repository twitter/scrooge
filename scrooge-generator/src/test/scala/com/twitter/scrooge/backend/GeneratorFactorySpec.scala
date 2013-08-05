package com.twitter.scrooge.backend

import org.specs.SpecificationWithJUnit
import com.twitter.scrooge.frontend.ResolvedDocument

class TestGeneratorFactory extends GeneratorFactory {
  val lang = "test"
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    generationDate: String,
    enablePassthrough: Boolean,
    experimentFlags: Seq[String]
  ): ThriftGenerator = new ScalaGenerator(includeMap, defaultNamespace, generationDate, enablePassthrough, experimentFlags)
}

class GeneratorFactorySpec extends SpecificationWithJUnit {
  "GeneratorFactory" should {
    "be loadable" in {
      val generator = Generator("test", Map.empty[String, ResolvedDocument], "", "", false, Seq.empty[String])
      generator must haveClass[ScalaGenerator]
    }
  }
}
