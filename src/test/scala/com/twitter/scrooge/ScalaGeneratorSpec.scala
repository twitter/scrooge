package com.twitter.scrooge

import java.math.BigInteger
import java.security.MessageDigest
import scala.collection.JavaConversions._
import com.twitter.util.Eval
import org.specs.Specification

class ScalaGeneratorSpec extends Specification {
  import AST._
  import ScalaGenerator.ConstList

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  def wrapInClass(name: String, code: String) = {
    "class " + name + " extends (() => Any) {" +
    "  def apply() = {" +
        code +
    "  }" +
    "}"
  }

  private def uniqueId(code: String): String = {
    val digest = MessageDigest.getInstance("SHA-1").digest(code.getBytes())
    val sha = new BigInteger(1, digest).toString(16)
    "Test_" + sha
  }

  def invokeTo[T](code: String): T = {
    Eval.compiler(wrapInClass(uniqueId(code), code))
    Eval.compiler.classLoader.loadClass(uniqueId(code)).newInstance.asInstanceOf[() => Any].apply().asInstanceOf[T]
  }

  def invoke(code: String): Any = invokeTo[Any](code)

  def compile(code: String) {
    Eval.compiler(code)
  }

  "ScalaGenerator" should {
    doBefore {
      Eval.compiler.reset()
    }

    "generate an enum" in {
      val enum = Enum("SomeEnum", Array(EnumValue("FOO", 1), EnumValue("BAR", 2)))
      compile(gen(enum))
      invoke("awwYeah.SomeEnum.FOO.value") mustEqual 1
      invoke("awwYeah.SomeEnum.BAR.value") mustEqual 2
      invoke("awwYeah.SomeEnum.apply(1)") mustEqual invoke("Some(awwYeah.SomeEnum.FOO)")
      invoke("awwYeah.SomeEnum.apply(2)") mustEqual invoke("Some(awwYeah.SomeEnum.BAR)")
      invoke("awwYeah.SomeEnum.apply(3)") mustEqual invoke("None")
    }

    "generate a constant" in {
      val constList = ConstList(Array(
        Const("name", TString, StringConstant("Columbo")),
        Const("someInt", TI32, IntConstant(1)),
        Const("someDouble", TDouble, DoubleConstant(3.0)),
        Const("someList", ListType(TString, None), ListConstant(List(StringConstant("piggy")))),
        Const("someMap", MapType(TString, TString, None), MapConstant(Map(StringConstant("foo") -> StringConstant("bar"))))
      ))
      compile(gen(constList))
      invoke("awwYeah.Constants.name") mustEqual "Columbo"
      invoke("awwYeah.Constants.someInt") mustEqual 1
      invoke("awwYeah.Constants.someDouble") mustEqual 3.0
      invoke("awwYeah.Constants.someList") mustEqual List("piggy")
      invoke("awwYeah.Constants.someMap") mustEqual Map("foo" -> "bar")
    }
  }
}
