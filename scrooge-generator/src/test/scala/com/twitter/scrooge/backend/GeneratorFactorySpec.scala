package com.twitter.scrooge.backend

import com.twitter.scrooge.ResolvedDocument
import org.specs.SpecificationWithJUnit

class TestGeneratorFactory extends GeneratorFactory {
  val lang = "test"
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    generationDate: String,
    enablePassthrough: Boolean
  ): ThriftGenerator = new ScalaGenerator(includeMap, defaultNamespace, generationDate, enablePassthrough)
}

class GeneratorFactorySpec extends SpecificationWithJUnit {
  "GeneratorFactory" should {
    "be loadable" in {
      val generator = Generator("test", Map.empty[String, ResolvedDocument], "", "", false)
      generator must haveClass[ScalaGenerator]
    }
  }
}
