package com.twitter.scrooge

import com.twitter.util.Eval
import org.specs.Specification
import scala.collection.JavaConversions._

class ScalaGeneratorSpec extends Specification {
  import AST._

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  def getClass(className: String) = Eval.compiler.classLoader.loadClass(className)

  def invoke[T](cls: Class[_], methodName: String): T =
    cls.getMethod(methodName).invoke(cls.newInstance()).asInstanceOf[T]

  def invoke[T, A1: Manifest](cls: Class[_], methodName: String, arg1: A1): T =
    cls.getMethod(methodName, manifest[A1].erasure).invoke(cls.newInstance(), arg1.asInstanceOf[Object]).asInstanceOf[T]

  def invoke[T](className: String, methodName: String): T =
    invoke[T](getClass(className), methodName)

  def invoke[T, A1: Manifest](className: String, methodName: String, arg1: A1): T =
    invoke[T, A1](getClass(className), methodName, arg1)


  "ScalaGenerator" should {
    "generate an enum" in {
      val enum = Enum("SomeEnum", Array(EnumValue("FOO", 1), EnumValue("BAR", 2)))
      println(gen(enum))
      try {
        Eval.compiler(gen(enum))
        invoke[Int]("awwYeah.SomeEnum$FOO$", "value") mustEqual 1
//        println(Eval.compiler.classLoader.loadClass("awwYeah.SomeEnum$").getMethods.toList.mkString("\n"))
//        println(Eval.compiler.classLoader.loadClass("awwYeah.SomeEnum$").getMethod("apply", classOf[Int]))
//        invoke[Option[AnyRef], Int]("awwYeah.SomeEnum$", "apply", 1).get.getClass.getName mustEqual "awwYeah.SomeEnum$FOO$"
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }
  }

}
