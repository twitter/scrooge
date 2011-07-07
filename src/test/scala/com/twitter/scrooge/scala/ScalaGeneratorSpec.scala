package com.twitter.scrooge
package scalagen

import java.nio.ByteBuffer
import scala.collection.JavaConversions._
import com.twitter.util.Eval
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}
import org.apache.thrift.protocol._
import javax.management.openmbean.TabularType

class ScalaGeneratorSpec extends Specification with JMocker with ClassMocker {
  import AST._
  import ScalaGenerator._

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  case class matchEqualsTField(a: TField) extends Matcher[TField]() {
    def apply(v: => TField) = (v.equals(a), "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTList(a: TList) extends Matcher[TList]() {
    def apply(v: => TList) = (v.elemType == a.elemType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTSet(a: TSet) extends Matcher[TSet]() {
    def apply(v: => TSet) = (v.elemType == a.elemType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTMap(a: TMap) extends Matcher[TMap]() {
    def apply(v: => TMap) = (v.keyType == a.keyType && v.valueType == a.valueType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  def equal(a: TField) = will(matchEqualsTField(a))
  def equal(a: TList) = will(matchEqualsTList(a))
  def equal(a: TSet) = will(matchEqualsTSet(a))
  def equal(a: TMap) = will(matchEqualsTMap(a))

  val protocol = mock[TProtocol]
  val eval = new Eval

  "ScalaGenerator" should {

    def invoke(code: String): Any = eval.inPlace[Any](code)

    def compile(code: String) {
      eval.compile(code)
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
      def startRead(protocol: TProtocol, field: TField) {
        one(protocol).readStructBegin()
        one(protocol).readFieldBegin() willReturn field
      }

      def nextRead(protocol: TProtocol, field: TField) {
        one(protocol).readFieldEnd()
        one(protocol).readFieldBegin() willReturn field
      }

      def endRead(protocol: TProtocol) {
        one(protocol).readFieldEnd()
        one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
        one(protocol).readStructEnd()
      }

      def startWrite(protocol: TProtocol, field: TField) {
        val s = capturingParam[TStruct]
        one(protocol).writeStructBegin(s.capture)
        one(protocol).writeFieldBegin(equal(field))
      }

      def nextWrite(protocol: TProtocol, field: TField) {
        one(protocol).writeFieldEnd()
        one(protocol).writeFieldBegin(equal(field))
      }

      def endWrite(protocol: TProtocol) {
        one(protocol).writeFieldEnd()
        one(protocol).writeFieldStop()
        one(protocol).writeStructEnd()
      }

      "ints" in {
        val struct = new Struct("Ints", Array(
          Field(1, "baby", TI16, None, Requiredness.Default),
          Field(2, "mama", TI32, None, Requiredness.Default),
          Field(3, "papa", TI64, None, Requiredness.Default)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("baby", TType.I16, 1))
            one(protocol).readI16() willReturn (16: Short)
            nextRead(protocol, new TField("mama", TType.I32, 2))
            one(protocol).readI32() willReturn 32
            nextRead(protocol, new TField("papa", TType.I64, 3))
            one(protocol).readI64() willReturn 64L
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Ints.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.Ints(16, 32, 64L)")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("baby", TType.I16, 1))
            one(protocol).writeI16(16)
            nextWrite(protocol, new TField("mama", TType.I32, 2))
            one(protocol).writeI32(32)
            nextWrite(protocol, new TField("papa", TType.I64, 3))
            one(protocol).writeI64(64)
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Ints(16, 32, 64L)").write(protocol)
        }
      }

      "bytes" in {
        val struct = new Struct("Bytes", Array(
          Field(1, "x", TByte, None, Requiredness.Default),
          Field(2, "y", TBinary, None, Requiredness.Default)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).readByte() willReturn 3.toByte
            nextRead(protocol, new TField("y", TType.STRING, 2))
            one(protocol).readBinary() willReturn ByteBuffer.wrap("hello".getBytes)
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Bytes.decoder")
          val obj = decoder(protocol)
          obj.getClass.getMethod("x").invoke(obj) mustEqual 3.toByte
          new String(obj.getClass.getMethod("y").invoke(obj).asInstanceOf[Array[Byte]]) mustEqual "hello"
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).writeByte(16.toByte)
            nextWrite(protocol, new TField("y", TType.STRING, 2))
            one(protocol).writeBinary(ByteBuffer.wrap("goodbye".getBytes))
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Bytes(16.toByte, \"goodbye\".getBytes)").write(protocol)
        }
      }

      "bool, double, string" in {
        val struct = new Struct("Misc", Array(
          Field(1, "alive", TBool, None, Requiredness.Default),
          Field(2, "pi", TDouble, None, Requiredness.Default),
          Field(3, "name", TString, None, Requiredness.Default)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("alive", TType.BOOL, 1))
            one(protocol).readBool() willReturn true
            nextRead(protocol, new TField("pi", TType.DOUBLE, 2))
            one(protocol).readDouble() willReturn 3.14
            nextRead(protocol, new TField("name", TType.STRING, 3))
            one(protocol).readString() willReturn "bender"
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Misc.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.Misc(true, 3.14, \"bender\")")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("alive", TType.BOOL, 1))
            one(protocol).writeBool(false)
            nextWrite(protocol, new TField("pi", TType.DOUBLE, 2))
            one(protocol).writeDouble(6.28)
            nextWrite(protocol, new TField("name", TType.STRING, 3))
            one(protocol).writeString("fry")
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Misc(false, 6.28, \"fry\")").write(protocol)
        }
      }

      "lists, sets, and maps" in {
        val struct = new Struct("Compound", Array(
          Field(1, "intlist", ListType(TI32, None), None, Requiredness.Default),
          Field(2, "intset", SetType(TI32, None), None, Requiredness.Default),
          Field(3, "namemap", MapType(TString, TI32, None), None, Requiredness.Default),
          Field(4, "nested", ListType(SetType(TI32, None), None), None, Requiredness.Default)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("intlist", TType.LIST, 1))
            one(protocol).readListBegin() willReturn new TList(TType.I32, 2)
            one(protocol).readI32() willReturn 10
            one(protocol).readI32() willReturn 20
            one(protocol).readListEnd()
            nextRead(protocol, new TField("intset", TType.SET, 2))
            one(protocol).readSetBegin() willReturn new TSet(TType.I32, 2)
            one(protocol).readI32() willReturn 44
            one(protocol).readI32() willReturn 55
            one(protocol).readSetEnd()
            nextRead(protocol, new TField("namemap", TType.MAP, 3))
            one(protocol).readMapBegin() willReturn new TMap(TType.STRING, TType.I32, 1)
            one(protocol).readString() willReturn "wendy"
            one(protocol).readI32() willReturn 500
            one(protocol).readMapEnd()
            nextRead(protocol, new TField("nested", TType.LIST, 4))
            one(protocol).readListBegin() willReturn new TList(TType.SET, 1)
            one(protocol).readSetBegin() willReturn new TSet(TType.I32, 1)
            one(protocol).readI32() willReturn 9
            one(protocol).readSetEnd()
            one(protocol).readListEnd()
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Compound.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.Compound(List(10, 20), Set(44, 55), Map(\"wendy\" -> 500), List(Set(9)))")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("intlist", TType.LIST, 1))
            one(protocol).writeListBegin(equal(new TList(TType.I32, 2)))
            one(protocol).writeI32(10)
            one(protocol).writeI32(20)
            one(protocol).writeListEnd()
            nextWrite(protocol, new TField("intset", TType.SET, 2))
            one(protocol).writeSetBegin(equal(new TSet(TType.I32, 2)))
            one(protocol).writeI32(44)
            one(protocol).writeI32(55)
            one(protocol).writeSetEnd()
            nextWrite(protocol, new TField("namemap", TType.MAP, 3))
            one(protocol).writeMapBegin(equal(new TMap(TType.STRING, TType.I32, 1)))
            one(protocol).writeString("wendy")
            one(protocol).writeI32(500)
            one(protocol).writeMapEnd()
            nextWrite(protocol, new TField("nested", TType.LIST, 4))
            one(protocol).writeListBegin(equal(new TList(TType.SET, 1)))
            one(protocol).writeSetBegin(equal(new TSet(TType.I32, 1)))
            one(protocol).writeI32(9)
            one(protocol).writeSetEnd()
            one(protocol).writeListEnd()
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Compound(List(10, 20), Set(44, 55), Map(\"wendy\" -> 500), List(Set(9)))").write(protocol)
        }
      }

      "with optional fields" in {
        val struct = new Struct("Optional", Array(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "age", TI32, None, Requiredness.Optional)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            nextRead(protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32() willReturn 14
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Optional.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.Optional(\"Commie\", Some(14))")
        }

        "read with missing field" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Optional.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.Optional(\"Commie\", None)")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            nextWrite(protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(14)
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Optional(\"Commie\", Some(14))").write(protocol)
        }

        "write with missing field" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Optional(\"Commie\", None)").write(protocol)
        }
      }

      "with required fields" in {
        val struct = new Struct("Required", Array(
          Field(1, "size", TI32, None, Requiredness.Required)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("size", TType.I32, 1))
            one(protocol).readI32() willReturn 23
            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Required.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.Required(23)")
        }

        "read with missing field" in {
          expect {
            one(protocol).readStructBegin()
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Required.decoder")
          decoder(protocol) must throwA[TProtocolException]
        }
      }

      "with default values" in {
        val struct = new Struct("DefaultValues", Array(
          Field(1, "name", TString, Some(StringConstant("leela")), Requiredness.Optional)
        ))

        compile(gen(struct))

        "read" in {
          expect {
            one(protocol).readStructBegin()
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.DefaultValues.decoder")
          decoder(protocol) mustEqual invoke("new awwYeah.DefaultValues(\"leela\")")
        }
      }

      "nested" in {
        val emperorStruct = new Struct("Emperor", Array(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "age", TI32, None, Requiredness.Default)
        ))
        val struct = new Struct("Empire", Array(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "provinces", ListType(TString, None), None, Requiredness.Default),
          Field(5, "emperor", ReferenceType("Emperor"), None, Requiredness.Default)
        ))

        compile(gen(emperorStruct))
        compile(gen(struct))

        "read" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "United States of America"
            nextRead(protocol, new TField("provinces", TType.LIST, 2))
            one(protocol).readListBegin() willReturn new TList(TType.STRING, 2)
            one(protocol).readString() willReturn "connecticut"
            one(protocol).readString() willReturn "california"
            one(protocol).readListEnd()
            nextRead(protocol, new TField("emperor", TType.STRUCT, 5))

            /** Start of Emperor struct **/
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Bush"
            nextRead(protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32() willReturn 42
            endRead(protocol)
            /** End of Emperor struct **/

            endRead(protocol)
          }

          val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Empire.decoder")
          decoder(protocol) mustEqual
            invoke("new awwYeah.Empire(\"United States of America\", List(\"connecticut\", \"california\"), awwYeah.Emperor(\"Bush\", 42))")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Canada")
            nextWrite(protocol, new TField("provinces", TType.LIST, 2))
            one(protocol).writeListBegin(equal(new TList(TType.STRING, 2)))
            one(protocol).writeString("Manitoba")
            one(protocol).writeString("Alberta")
            one(protocol).writeListEnd()
            nextWrite(protocol, new TField("emperor", TType.STRUCT, 5))

            // emperor
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Larry")
            nextWrite(protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(13)
            endWrite(protocol)

            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Empire(\"Canada\", List(\"Manitoba\", \"Alberta\"), awwYeah.Emperor(\"Larry\", 13))").write(protocol)
        }
      }
    }
  }
}
