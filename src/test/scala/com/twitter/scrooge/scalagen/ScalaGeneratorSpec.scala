package com.twitter.scrooge
package scalagen

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.JavaConversions._
import com.twitter.util.Eval
import org.apache.thrift.protocol._
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}

class ScalaGeneratorSpec extends Specification with EvalHelper with JMocker with ClassMocker {
  import AST._
  import ScalaGenerator._

  type ThriftStruct = { def write(oprot: TProtocol) }

  val gen = new ScalaGenerator
  val doc = new Document(Seq(Namespace("scala", "awwYeah")), Nil)
  val protocol = mock[TProtocol]

  "ScalaGenerator" should {
    "generate an enum" in {
      val enum = Enum("SomeEnum", Seq(EnumValue("FOO", 1), EnumValue("BAR", 2)))
      compile(gen(doc, enum))
      invoke("awwYeah.SomeEnum.FOO.value") mustEqual 1
      invoke("awwYeah.SomeEnum.BAR.value") mustEqual 2
      invoke("awwYeah.SomeEnum.get(1)") mustEqual invoke("Some(awwYeah.SomeEnum.FOO)")
      invoke("awwYeah.SomeEnum.get(2)") mustEqual invoke("Some(awwYeah.SomeEnum.BAR)")
      invoke("awwYeah.SomeEnum.get(3)") mustEqual invoke("None")
      invoke("awwYeah.SomeEnum(1)") mustEqual invoke("awwYeah.SomeEnum.FOO")
      invoke("awwYeah.SomeEnum(2)") mustEqual invoke("awwYeah.SomeEnum.BAR")
      invoke("awwYeah.SomeEnum(3)") must throwA[NoSuchElementException]
    }

    "generate a constant" in {
      val constList = ConstList(Seq(
        Const("name", TString, StringConstant("Columbo")),
        Const("someInt", TI32, IntConstant(1)),
        Const("someDouble", TDouble, DoubleConstant(3.0)),
        Const("someList", ListType(TString, None), ListConstant(Seq(StringConstant("piggy")))),
        Const("someMap", MapType(TString, TString, None), MapConstant(Map(StringConstant("foo") -> StringConstant("bar")))),
        Const("alias", ReferenceType("FakeEnum"), Identifier("FOO"))
      ))
      // add a definition for SomeEnum2.FOO so it will compile.
      val code = gen(doc, constList) + "\n\nclass FakeEnum()\nobject FakeEnum { val FOO = new FakeEnum() }\n"
      compile(code)

      invoke("awwYeah.Constants.name") mustEqual "Columbo"
      invoke("awwYeah.Constants.someInt") mustEqual 1
      invoke("awwYeah.Constants.someDouble") mustEqual 3.0
      invoke("awwYeah.Constants.someList") mustEqual List("piggy")
      invoke("awwYeah.Constants.someMap") mustEqual Map("foo" -> "bar")
      invoke("awwYeah.Constants.alias") mustEqual invoke("awwYeah.FakeEnum.FOO")
    }

    "generate a struct" in {
      "ints" in {
        val struct = new Struct("Ints", Seq(
          Field(1, "baby", TI16, None, Requiredness.Default),
          Field(2, "mama", TI32, None, Requiredness.Default),
          Field(3, "papa", TI64, None, Requiredness.Default)
        ))

        compile(gen(doc, struct))

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
        val struct = new Struct("Bytes", Seq(
          Field(1, "x", TByte, None, Requiredness.Default),
          Field(2, "y", TBinary, None, Requiredness.Default)
        ))

        compile(gen(doc, struct))

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
          new String(obj.getClass.getMethod("y").invoke(obj).asInstanceOf[ByteBuffer].array) mustEqual "hello"
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).writeByte(16.toByte)
            nextWrite(protocol, new TField("y", TType.STRING, 2))
            one(protocol).writeBinary(ByteBuffer.wrap("goodbye".getBytes))
            endWrite(protocol)
          }

          eval.inPlace[ThriftStruct]("awwYeah.Bytes(16.toByte, java.nio.ByteBuffer.wrap(\"goodbye\".getBytes))").write(protocol)
        }
      }

      "bool, double, string" in {
        val struct = new Struct("Misc", Seq(
          Field(1, "alive", TBool, None, Requiredness.Default),
          Field(2, "pi", TDouble, None, Requiredness.Default),
          Field(3, "name", TString, None, Requiredness.Default)
        ))

        compile(gen(doc, struct))

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
        val struct = new Struct("Compound", Seq(
          Field(1, "intlist", ListType(TI32, None), None, Requiredness.Default),
          Field(2, "intset", SetType(TI32, None), None, Requiredness.Default),
          Field(3, "namemap", MapType(TString, TI32, None), None, Requiredness.Default),
          Field(4, "nested", ListType(SetType(TI32, None), None), None, Requiredness.Default)
        ))

        compile(gen(doc, struct))

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
        val struct = new Struct("Optional", Seq(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "age", TI32, None, Requiredness.Optional)
        ))

        compile(gen(doc, struct))

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
        val struct = new Struct("Required", Seq(
          Field(1, "size", TI32, None, Requiredness.Required)
        ))

        compile(gen(doc, struct))

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
        val struct = new Struct("DefaultValues", Seq(
          Field(1, "name", TString, Some(StringConstant("leela")), Requiredness.Optional)
        ))

        compile(gen(doc, struct))

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
        val emperorStruct = new Struct("Emperor", Seq(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "age", TI32, None, Requiredness.Default)
        ))
        val struct = new Struct("Empire", Seq(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "provinces", ListType(TString, None), None, Requiredness.Default),
          Field(5, "emperor", StructType(emperorStruct), None, Requiredness.Default)
        ))

        compile(gen(doc, emperorStruct))
        compile(gen(doc, struct))

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

      "exception" in {
        val error = new Exception_("Error", Seq(
          Field(1, "description", TString, None, Requiredness.Default)
        ))

        compile(gen(doc, error))
        invoke("new awwYeah.Error(\"silly\").getStackTrace") must haveClass[Array[StackTraceElement]]
      }
    }
  }
}
