package com.twitter.scrooge

import com.twitter.util.Eval
import org.specs.Specification
import scala.collection.JavaConversions._

class ScalaGeneratorSpec extends Specification {
  import AST._

  var counter = 0

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  def wrapInClass(name: String, code: String) = {
    "class " + name + " extends (() => Any) {" +
    "  def apply() = {" +
        code +
    "  }" +
    "}"
  }

  def invokeTo[T](code: String): T = {
    counter += 1
    Eval.compiler(wrapInClass("Test" + counter, code))
    Eval.compiler.classLoader.loadClass("Test" + counter).newInstance.asInstanceOf[() => Any].apply().asInstanceOf[T]
  }

  def invoke(code: String): Any = invokeTo[Any](code)

  def compile(code: String) {
    Eval.compiler(code)
  }

  "ScalaGenerator" should {
    "generate an enum" in {
      val enum = Enum("SomeEnum", Array(EnumValue("FOO", 1), EnumValue("BAR", 2)))
      compile(gen(enum))
      invoke("awwYeah.SomeEnum.FOO.value") mustEqual 1
      invoke("awwYeah.SomeEnum.BAR.value") mustEqual 2
      invoke("awwYeah.SomeEnum.apply(1)") mustEqual invoke("Some(awwYeah.SomeEnum.FOO)")
      invoke("awwYeah.SomeEnum.apply(2)") mustEqual invoke("Some(awwYeah.SomeEnum.BAR)")
      invoke("awwYeah.SomeEnum.apply(3)") mustEqual invoke("None")
    }
  }
}