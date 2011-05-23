package com.twitter.scrooge

import java.math.BigInteger
import java.security.MessageDigest
import scala.collection.JavaConversions._
import com.twitter.util.Eval
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}
import org.apache.thrift.protocol.{TStruct, TType, TField, TProtocol, TList}
import javax.jws.Oneway
import javax.management.openmbean.TabularType

class ScalaGeneratorSpec extends Specification with JMocker with ClassMocker {
  import AST._
  import ScalaGenerator._

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  case class matchEqualsTField(a: TField) extends Matcher[TField]() {
    def apply(v: => TField) = (v.equals(a), "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  def equal(a: TField) = will(matchEqualsTField(a))

  val protocol = mock[TProtocol]


  "ScalaGenerator" should {
    var eval: Eval = null

    def invoke(code: String): Any = eval.inPlace[Any](code)

    def compile(code: String) {
      eval.compile(code)
    }

    doBefore {
      eval = new Eval
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
        Const("someList", ListType(TString, None), ListConstant(Array(StringConstant("piggy")))),
        Const("someMap", MapType(TString, TString, None), MapConstant(Map(StringConstant("foo") -> StringConstant("bar")))),
        Const("alias", ReferenceType("FakeEnum"), Identifier("FOO"))
      ))
      // add a definition for SomeEnum2.FOO so it will compile.
      val code = gen(constList) + "\n\nclass FakeEnum()\nobject FakeEnum { val FOO = new FakeEnum() }\n"
      compile(code)

      invoke("awwYeah.Constants.name") mustEqual "Columbo"
      invoke("awwYeah.Constants.someInt") mustEqual 1
      invoke("awwYeah.Constants.someDouble") mustEqual 3.0
      invoke("awwYeah.Constants.someList") mustEqual List("piggy")
      invoke("awwYeah.Constants.someMap") mustEqual Map("foo" -> "bar")
      invoke("awwYeah.Constants.alias") mustEqual invoke("awwYeah.FakeEnum.FOO")
    }

    "generate a struct" in {
      "simple" in {
        val struct = new Struct("Foo", Array(
          Field(1, "bar", TI32, None, Requiredness.Optional),
          Field(2, "baz", TString, None, Requiredness.Optional)
        ))
        val structString = gen(struct)
        compile(structString)

        expect {
          one(protocol).readStructBegin()

          one(protocol).readFieldBegin() willReturn new TField("bar", TType.I32, 1)
          one(protocol).readI32() willReturn 1
          one(protocol).readFieldEnd()

          one(protocol).readFieldBegin() willReturn new TField("baz", TType.STRING, 2)
          one(protocol).readString() willReturn "lala"
          one(protocol).readFieldEnd()

          one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
          one(protocol).readStructEnd()
        }

        val s = capturingParam[TStruct]

        val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Foo.decoder")
        decoder(protocol) mustEqual invoke("new awwYeah.Foo(1, \"lala\")")

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

        eval.inPlace[ThriftStruct]("awwYeah.Foo(1, \"lala\")").write(protocol)
      }
    }

    "nested" in {
      val emperorStruct = new Struct("Emperor", Array(
        Field(1, "name", TString, None, Requiredness.Optional),
        Field(2, "age", TI32, None, Requiredness.Optional)
      ))
      val struct = new Struct("Empire", Array(
        Field(1, "name", TString, None, Requiredness.Optional),
        Field(2, "provinces", ListType(TString, None), None, Requiredness.Optional),
        Field(5, "emperor", ReferenceType("Emperor"), None, Requiredness.Optional)
      ))

      compile(gen(emperorStruct))
      compile(gen(struct))

      val s = capturingParam[TStruct]

      expect {
        one(protocol).readStructBegin()

        one(protocol).readFieldBegin() willReturn new TField("name", TType.STRING, 1)
        one(protocol).readString() willReturn "United States of America"
        one(protocol).readFieldEnd()

        one(protocol).readFieldBegin() willReturn new TField("provinces", TType.LIST, 2)
        one(protocol).readListBegin() willReturn new TList(TType.STRING, 2)
        one(protocol).readString() willReturn "connecticut"
        one(protocol).readString() willReturn "california"
        one(protocol).readListEnd()
        one(protocol).readFieldEnd()

        one(protocol).readFieldBegin() willReturn new TField("emperor", TType.STRUCT, 5)
        /** Start of Emperor struct **/
        one(protocol).readStructBegin()

        one(protocol).readFieldBegin() willReturn new TField("name", TType.STRING, 1)
        one(protocol).readString() willReturn "Bush"
        one(protocol).readFieldEnd()

        one(protocol).readFieldBegin() willReturn new TField("age", TType.I32, 2)
        one(protocol).readI32() willReturn 42
        one(protocol).readFieldEnd()

        one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
        one(protocol).readStructEnd()
        /** End of Emperor struct **/
        one(protocol).readFieldEnd()

        one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
        one(protocol).readStructEnd()
      }

      val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Empire.decoder")
      decoder(protocol) mustEqual
        invoke("new awwYeah.Empire(\"United States of America\", List(\"connecticut\", \"california\"), awwYeah.Emperor(\"Bush\", 42))")
    }
  }
}
