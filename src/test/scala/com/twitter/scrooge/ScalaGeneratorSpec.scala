package com.twitter.scrooge

import java.math.BigInteger
import java.security.MessageDigest
import scala.collection.JavaConversions._
import com.twitter.util.Eval
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}
import org.apache.thrift.protocol.{TStruct, TType, TField, TProtocol}

class ScalaGeneratorSpec extends Specification with JMocker with ClassMocker {
  import AST._
  import ScalaGenerator._

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

    "generate a struct" in {
      val struct = new Struct("Foo", Array(Field(1, "bar", TI32, None, false), Field(2, "baz", TString, None, false)))
      val structString = gen(struct)
      println(structString)
      compile(structString)

      case class matchEqualsTField(a: TField) extends Matcher[TField]() {
        def apply(v: => TField) = (v.equals(a), "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
      }

      def equal(a: TField) = will(matchEqualsTField(a))

      val protocol = mock[TProtocol] 

      expect {
        one(protocol).readFieldBegin willReturn new TField("bar", TType.I32, 1)
        one(protocol).readI32 willReturn 1
        one(protocol).readFieldEnd()

        one(protocol).readFieldBegin willReturn new TField("baz", TType.STRING, 2)
        one(protocol).readString willReturn "lala"
        one(protocol).readFieldEnd()
        
        one(protocol).readFieldBegin willReturn new TField("stop", TType.STOP, 10)
        one(protocol).readStructEnd()
      }

      val s = capturingParam[TStruct]

      val foo = invokeTo[(TProtocol => ThriftStruct)]("awwYeah.Foo")
      foo(protocol) mustEqual invoke("awwYeah.Foo(1, \"lala\")")

      expect {
        one(protocol).writeStructBegin(s.capture)

        one(protocol).writeFieldBegin(equal(new TField("bar", TType.I32, 1)))
        one(protocol).writeI32(1)
        one(protocol).writeFieldEnd()

        one(protocol).writeFieldBegin(equal(new TField("baz", TType.STRING, 2)))
        one(protocol).writeString("lala")
        one(protocol).writeFieldEnd()

        one(protocol).writeFieldStop()
        one(protocol).writeStructEnd()
      }

      invokeTo[ThriftStruct]("awwYeah.Foo(1, \"lala\")").write(protocol)
    }
  }
}
