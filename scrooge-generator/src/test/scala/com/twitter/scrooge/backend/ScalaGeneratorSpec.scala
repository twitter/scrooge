package com.twitter.scrooge.backend

import com.twitter.finagle.SourcedException
import com.twitter.scrooge.testutil.{EvalHelper, JMockSpec}
import com.twitter.scrooge.{ThriftStruct, ThriftException}
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import org.jmock.Expectations
import org.jmock.Expectations.{any, returnValue}
import thrift.test._
import thrift.test1._
import thrift.test2._
import thrift.`def`.default._

class ScalaGeneratorSpec extends JMockSpec with EvalHelper {
  def stringToBytes(string: String) = ByteBuffer.wrap(string.getBytes)

  "ScalaGenerator" should {
    "generate an enum" should {
      "correct constants" in { _ =>
        NumberID.One.getValue must be(1)
        NumberID.Two.getValue must be(2)
        NumberID.Three.getValue must be(3)
        NumberID.Five.getValue must be(5)
        NumberID.Six.getValue must be(6)
        NumberID.Eight.getValue must be(8)
      }

      "correct names" in { _ =>
        NumberID.One.name must be("One")
        NumberID.Two.name must be("Two")
        NumberID.Three.name must be("Three")
        NumberID.Five.name must be("Five")
        NumberID.Six.name must be("Six")
        NumberID.Eight.name must be("Eight")
      }

      "apply" in { _ =>
        NumberID(1) must be(NumberID.One)
        NumberID(2) must be(NumberID.Two)
        NumberID(3) must be(NumberID.Three)
        NumberID(5) must be(NumberID.Five)
        NumberID(6) must be(NumberID.Six)
        NumberID(8) must be(NumberID.Eight)
      }

      "get" in { _ =>
        NumberID.get(1) must be(Some(NumberID.One))
        NumberID.get(2) must be(Some(NumberID.Two))
        NumberID.get(3) must be(Some(NumberID.Three))
        NumberID.get(5) must be(Some(NumberID.Five))
        NumberID.get(6) must be(Some(NumberID.Six))
        NumberID.get(8) must be(Some(NumberID.Eight))
        NumberID.get(10) must be(None)
      }

      "valueOf" in { _ =>
        NumberID.valueOf("One") must be(Some(NumberID.One))
        NumberID.valueOf("Two") must be(Some(NumberID.Two))
        NumberID.valueOf("Three") must be(Some(NumberID.Three))
        NumberID.valueOf("Five") must be(Some(NumberID.Five))
        NumberID.valueOf("Six") must be(Some(NumberID.Six))
        NumberID.valueOf("Eight") must be(Some(NumberID.Eight))
        NumberID.valueOf("Ten") must be(None)
      }

      "correct list" in { _ =>
        NumberID.list(0) must be(NumberID.One)
        NumberID.list(1) must be(NumberID.Two)
        NumberID.list(2) must be(NumberID.Three)
        NumberID.list(3) must be(NumberID.Five)
        NumberID.list(4) must be(NumberID.Six)
        NumberID.list(5) must be(NumberID.Eight)
        NumberID.list.size must be(6)
      }

      "java-serializable" in { _ =>
        val bos = new ByteArrayOutputStream()
        val out = new ObjectOutputStream(bos)
        out.writeObject(NumberID.One)
        out.writeObject(NumberID.Two)
        bos.close()
        val bytes = bos.toByteArray

        val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
        var obj = in.readObject()
        obj.isInstanceOf[NumberID] must be(true)
        obj.asInstanceOf[NumberID].getValue must be(NumberID.One.getValue)
        obj.asInstanceOf[NumberID].name must be(NumberID.One.name)

        obj = in.readObject()
        obj.isInstanceOf[NumberID] must be(true)
        obj.asInstanceOf[NumberID].getValue must be(NumberID.Two.getValue)
        obj.asInstanceOf[NumberID].name must be(NumberID.Two.name)
      }

      "handle namespace collisions" in { _ =>
        NamespaceCollisions.List.name must be("List")
        NamespaceCollisions.Any.name must be("Any")
        NamespaceCollisions.AnyRef.name must be("AnyRef")
        NamespaceCollisions.Object.name must be("Object")
        NamespaceCollisions.String.name must be("String")
        NamespaceCollisions.Byte.name must be("Byte")
        NamespaceCollisions.Short.name must be("Short")
        NamespaceCollisions.Char.name must be("Char")
        NamespaceCollisions.Int.name must be("Int")
        NamespaceCollisions.Long.name must be("Long")
        NamespaceCollisions.Float.name must be("Float")
        NamespaceCollisions.Double.name must be("Double")
        NamespaceCollisions.Option.name must be("Option")
        NamespaceCollisions.None.name must be("None")
        NamespaceCollisions.Some.name must be("Some")
        NamespaceCollisions.Nil.name must be("Nil")
        NamespaceCollisions.Null.name must be("Null")
        NamespaceCollisions.Set.name must be("Set")
        NamespaceCollisions.Map.name must be("Map")
        NamespaceCollisions.Seq.name must be("Seq")
        NamespaceCollisions.Array.name must be("Array")
        NamespaceCollisions.Iterable.name must be("Iterable")
        NamespaceCollisions.Unit.name must be("Unit")
        NamespaceCollisions.Nothing.name must be("Nothing")
        NamespaceCollisions.Protected.name must be("Protected")

        NamespaceCollisions.valueOf("null") must be(Some(NamespaceCollisions.Null))
        NamespaceCollisions.valueOf("protected") must be(Some(NamespaceCollisions.Protected))
      }

      "encode-decode in struct" in { _ =>
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val eStruct = EnumStruct(NumberID.One)
        EnumStruct.encode(eStruct, prot)
        EnumStruct.decode(prot) must be(eStruct)
      }

      "encode-decode in union" in { _ =>
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val eUnion = EnumUnion.Number(NumberID.One)
        EnumUnion.encode(eUnion, prot)
        EnumUnion.decode(prot) must be(eUnion)
      }

      "encode-decode union with unknown field" in { _ =>
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val unionField = NewUnionField(
          14653230,
          SomeInnerUnionStruct(26, "a_a")
        )
        val newUnion = UnionPostEvolution.NewField(unionField)
        UnionPostEvolution.encode(newUnion, prot)
        val decoded = UnionPreEvolution.decode(prot)
        decoded.isInstanceOf[UnionPreEvolution.UnknownUnionField] must be(true)

        val oldProt = new TBinaryProtocol(new TMemoryBuffer(64))
        UnionPreEvolution.encode(decoded, oldProt)
        val decodedNew = UnionPostEvolution.decode(oldProt)
        decodedNew must be(UnionPostEvolution.NewField(unionField))
      }

      "be identified as an ENUM" in { _ =>
        EnumStruct.NumberField.`type` must be(TType.ENUM)
      }

      "be identified as an I32 on the wire for structs" in { _ =>
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        EnumStruct.encode(EnumStruct(NumberID.One), prot)
        prot.readStructBegin()
        val field = prot.readFieldBegin()
        field.`type` must be(TType.I32)
      }

      "be identified as an I32 on the wire for unions" in { _ =>
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        EnumUnion.encode(EnumUnion.Number(NumberID.One), prot)
        prot.readStructBegin()
        val field = prot.readFieldBegin()
        field.`type` must be(TType.I32)
      }

      "be identified as I32 on the wire for collections" in { _ =>
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val eStruct = EnumCollections(
          aMap = Map(NumberID.One -> NumberID.Two),
          aList = List(NumberID.One),
          aSet = Set(NumberID.One))

        EnumCollections.encode(eStruct, prot)
        EnumCollections.decode(prot) must be(eStruct)

        EnumCollections.encode(eStruct, prot)

        prot.readStructBegin()

        // Test Map encoding
        prot.readFieldBegin()
        val mapField = prot.readMapBegin()
        mapField.keyType must be(TType.I32)
        mapField.valueType must be(TType.I32)
        prot.readI32(); prot.readI32()
        prot.readMapEnd()
        prot.readFieldEnd()

        // Test List encoding
        prot.readFieldBegin()
        val listField = prot.readListBegin()
        listField.elemType must be(TType.I32)
        prot.readI32()
        prot.readListEnd()
        prot.readFieldEnd()

        // Test Set encoding
        prot.readFieldBegin()
        val setField = prot.readSetBegin()
        setField.elemType must be(TType.I32)
      }
    }

    "generate constants" in { _ =>
      thrift.test.Constants.myWfhDay must be(WeekDay.Thu)
      thrift.test.Constants.myDaysOut must be(List(WeekDay.Thu, WeekDay.Sat, WeekDay.SUN))
      thrift.test.Constants.name must be("Columbo")
      thrift.test.Constants.someInt must be(1)
      thrift.test.Constants.someDouble must be(3.0)
      thrift.test.Constants.someList must be(List("piggy"))
      thrift.test.Constants.emptyList must be(List())
      thrift.test.Constants.someMap must be(Map("foo" -> "bar"))
      thrift.test.Constants.someSimpleSet must be(Set("foo", "bar"))
      thrift.test.Constants.someSet must be(Set(
        List("piggy"),
        List("kitty")
      ))
    }

    "basic structs" should {
      "ints" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("baby", TType.I16, 1))
            one(protocol).readI16(); will(returnValue((16: Short)))
            nextRead(e, protocol, new TField("mama", TType.I32, 2))
            one(protocol).readI32(); will(returnValue(32))
            nextRead(e, protocol, new TField("papa", TType.I64, 3))
            one(protocol).readI64(); will(returnValue(64L))
            endRead(e, protocol)
          }

          whenExecuting {
            Ints.decode(protocol) must be(Ints(16, 32, 64L))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("baby", TType.I16, 1))
            one(protocol).writeI16(`with`(Expectations.equal(16: Short)))
            nextWrite(e, protocol, new TField("mama", TType.I32, 2))
            one(protocol).writeI32(`with`(Expectations.equal(32)))
            nextWrite(e, protocol, new TField("papa", TType.I64, 3))
            one(protocol).writeI64(`with`(Expectations.equal(64L)))
            endWrite(e, protocol)
          }

          whenExecuting {
            Ints(16, 32, 64L).write(protocol) must be(())
          }
        }
      }

      "bytes" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("x", TType.BYTE, 1))
            one(protocol).readByte(); will(returnValue(3.toByte))
            nextRead(e, protocol, new TField("y", TType.STRING, 2))
            one(protocol).readBinary(); will(returnValue(stringToBytes("hello")))
            endRead(e, protocol)
          }

          whenExecuting {
            val bytes = Bytes.decode(protocol)
            bytes.x must be(3.toByte)
            new String(bytes.y.array) must be("hello")
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("x", TType.BYTE, 1))
            one(protocol).writeByte(`with`(Expectations.equal(16.toByte)))
            nextWrite(e, protocol, new TField("y", TType.STRING, 2))
            one(protocol).writeBinary(`with`(Expectations.equal(stringToBytes("goodbye"))))
            endWrite(e, protocol)
          }

          whenExecuting {
            Bytes(16.toByte, stringToBytes("goodbye")).write(protocol) must be(())
          }
        }
      }

      "bool, double, string" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("alive", TType.BOOL, 1))
            one(protocol).readBool(); will(returnValue(true))
            nextRead(e, protocol, new TField("pi", TType.DOUBLE, 2))
            one(protocol).readDouble(); will(returnValue(3.14))
            nextRead(e, protocol, new TField("name", TType.STRING, 3))
            one(protocol).readString(); will(returnValue("bender"))
            endRead(e, protocol)
          }

          whenExecuting {
            Misc.decode(protocol) must be(Misc(true, 3.14, "bender"))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("alive", TType.BOOL, 1))
            one(protocol).writeBool(`with`(Expectations.equal(false)))
            nextWrite(e, protocol, new TField("pi", TType.DOUBLE, 2))
            one(protocol).writeDouble(`with`(Expectations.equal(6.28)))
            nextWrite(e, protocol, new TField("name", TType.STRING, 3))
            one(protocol).writeString(`with`(Expectations.equal("fry")))
            endWrite(e, protocol)
          }

          whenExecuting {
            Misc(false, 6.28, "fry").write(protocol) must be(())
          }
        }
      }

      "lists, sets, and maps" should {
        val exemplar = Compound(
          intlist = List(10, 20),
          intset = Set(44, 55),
          namemap = Map("wendy" -> 500),
          nested = List(Set(9)))

        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("intlist", TType.LIST, 1))
            one(protocol).readListBegin(); will(returnValue(new TList(TType.I32, 2)))
            one(protocol).readI32(); will(returnValue(10))
            one(protocol).readI32(); will(returnValue(20))
            one(protocol).readListEnd()
            nextRead(e, protocol, new TField("intset", TType.SET, 2))
            one(protocol).readSetBegin(); will(returnValue(new TSet(TType.I32, 2)))
            one(protocol).readI32(); will(returnValue(44))
            one(protocol).readI32(); will(returnValue(55))
            one(protocol).readSetEnd()
            nextRead(e, protocol, new TField("namemap", TType.MAP, 3))
            one(protocol).readMapBegin(); will(returnValue(new TMap(TType.STRING, TType.I32, 1)))
            one(protocol).readString(); will(returnValue("wendy"))
            one(protocol).readI32(); will(returnValue(500))
            one(protocol).readMapEnd()
            nextRead(e, protocol, new TField("nested", TType.LIST, 4))
            one(protocol).readListBegin(); will(returnValue(new TList(TType.SET, 1)))
            one(protocol).readSetBegin(); will(returnValue(new TSet(TType.I32, 1)))
            one(protocol).readI32(); will(returnValue(9))
            one(protocol).readSetEnd()
            one(protocol).readListEnd()
            endRead(e, protocol)
          }

          whenExecuting {
            Compound.decode(protocol) must be(exemplar)
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("intlist", TType.LIST, 1))
            one(protocol).writeListBegin(`with`(listEqual(new TList(TType.I32, 2))))
            one(protocol).writeI32(`with`(Expectations.equal(10)))
            one(protocol).writeI32(`with`(Expectations.equal(20)))
            one(protocol).writeListEnd()
            nextWrite(e, protocol, new TField("intset", TType.SET, 2))
            one(protocol).writeSetBegin(`with`(setEqual(new TSet(TType.I32, 2))))
            one(protocol).writeI32(`with`(Expectations.equal(44)))
            one(protocol).writeI32(`with`(Expectations.equal(55)))
            one(protocol).writeSetEnd()
            nextWrite(e, protocol, new TField("namemap", TType.MAP, 3))
            one(protocol).writeMapBegin(`with`(mapEqual(new TMap(TType.STRING, TType.I32, 1))))
            one(protocol).writeString(`with`(Expectations.equal("wendy")))
            one(protocol).writeI32(`with`(Expectations.equal(500)))
            one(protocol).writeMapEnd()
            nextWrite(e, protocol, new TField("nested", TType.LIST, 4))
            one(protocol).writeListBegin(`with`(listEqual(new TList(TType.SET, 1))))
            one(protocol).writeSetBegin(`with`(setEqual(new TSet(TType.I32, 1))))
            one(protocol).writeI32(`with`(Expectations.equal(9)))
            one(protocol).writeSetEnd()
            one(protocol).writeListEnd()
            endWrite(e, protocol)
          }

          whenExecuting {
            exemplar.write(protocol) must be(())
          }
        }
      }
    }

    "complicated structs" should {
      "with required fields" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("string", TType.STRING, 1))
            one(protocol).readString(); will(returnValue("yo"))
            endRead(e, protocol)
          }

          whenExecuting {
            RequiredString.decode(protocol) must be(RequiredString("yo"))
          }
        }

        "missing required value throws exception during deserialization" should {
          "with no default value" in { cycle => import cycle._
            val protocol = mock[TProtocol]
            expecting { e => import e._
              emptyRead(e, protocol)
            }

            whenExecuting {
              intercept[TProtocolException] {
                RequiredString.decode(protocol)
              }
            }
          }

          "with default value" in { cycle => import cycle._
            val protocol = mock[TProtocol]
            expecting { e => import e._
              emptyRead(e, protocol)
            }

            whenExecuting {
              intercept[TProtocolException] {
                RequiredStringWithDefault.decode(protocol)
              }
            }
          }
        }

        "null required value throws exception during serialization" should {
          "with no default value" in { cycle => import cycle._
            val protocol = mock[TProtocol]

            whenExecuting {
              intercept[TProtocolException] {
                RequiredString(value = null).write(protocol)
              }
            }
          }

          "with default value" in { cycle => import cycle._
            val protocol = mock[TProtocol]

            whenExecuting {
              intercept[TProtocolException] {
                RequiredStringWithDefault(value = null).write(protocol)
              }
            }
          }
        }

        "wrong type" should {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))

          protocol.writeStructBegin(new TStruct("test"))
          protocol.writeFieldBegin(new TField("number", TType.I32, 1))
          protocol.writeI32(4000)
          protocol.writeFieldEnd()
          protocol.writeFieldStop()
          protocol.writeStructEnd()

          "throw an exception" in { _ =>
            val ex = intercept[TProtocolException] {
              RequiredString.decode(protocol)
            }

            ex.toString.contains("value") must be(true)
            ex.toString.contains("actual=I32") must be(true)
            ex.toString.contains("expected=STRING") must be(true)
          }
        }
      }

      "with optional fields" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue("Commie"))
            nextRead(e, protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32(); will(returnValue(14))
            endRead(e, protocol)
          }

          whenExecuting {
            OptionalInt.decode(protocol) must be(OptionalInt("Commie", Some(14)))
          }
        }

        "read with missing field" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue("Commie"))
            endRead(e, protocol)
          }

          whenExecuting {
            OptionalInt.decode(protocol) must be(OptionalInt("Commie", None))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString(`with`(Expectations.equal("Commie")))
            nextWrite(e, protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(`with`(Expectations.equal(14)))
            endWrite(e, protocol)
          }

          whenExecuting {
            OptionalInt("Commie", Some(14)).write(protocol) must be(())
          }
        }

        "write with missing field" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString(`with`(Expectations.equal("Commie")))
            endWrite(e, protocol)
          }

          whenExecuting {
            OptionalInt("Commie", None).write(protocol) must be(())
          }
        }
      }

      "with default values" should {
        "read with value missing, using default" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            one(protocol).readStructBegin()
            one(protocol).readFieldBegin(); will(returnValue(new TField("stop", TType.STOP, 10)))
            one(protocol).readStructEnd()
          }

          whenExecuting {
            DefaultValues.decode(protocol) must be(DefaultValues("leela"))
          }
        }

        "read with value present" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            one(protocol).readStructBegin()
            nextRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue("delilah"))
            one(protocol).readFieldBegin(); will(returnValue(new TField("stop", TType.STOP, 10)))
            one(protocol).readStructEnd()
          }

          whenExecuting {
            DefaultValues.decode(protocol) must be(DefaultValues("delilah"))
          }
        }
      }

      "nested" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue("United States of America"))
            nextRead(e, protocol, new TField("provinces", TType.LIST, 2))
            one(protocol).readListBegin(); will(returnValue(new TList(TType.STRING, 2)))
            one(protocol).readString(); will(returnValue("connecticut"))
            one(protocol).readString(); will(returnValue("california"))
            one(protocol).readListEnd()
            nextRead(e, protocol, new TField("emperor", TType.STRUCT, 5))

            /** Start of Emperor struct **/
            startRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue("Bush"))
            nextRead(e, protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32(); will(returnValue(42))
            endRead(e, protocol)
            /** End of Emperor struct **/

            endRead(e, protocol)
          }

          whenExecuting {
            Empire.decode(protocol) must be(Empire(
              "United States of America",
              List("connecticut", "california"),
              Emperor("Bush", 42)))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString(`with`(Expectations.equal("Canada")))
            nextWrite(e, protocol, new TField("provinces", TType.LIST, 2))
            one(protocol).writeListBegin(`with`(listEqual(new TList(TType.STRING, 2))))
            one(protocol).writeString(`with`(Expectations.equal("Manitoba")))
            one(protocol).writeString(`with`(Expectations.equal("Alberta")))
            one(protocol).writeListEnd()
            nextWrite(e, protocol, new TField("emperor", TType.STRUCT, 5))

            // emperor
            startWrite(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString(`with`(Expectations.equal("Larry")))
            nextWrite(e, protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(`with`(Expectations.equal(13)))
            endWrite(e, protocol)

            endWrite(e, protocol)
          }

          whenExecuting {
            Empire(
              "Canada",
              List("Manitoba", "Alberta"),
              Emperor("Larry", 13)
            ).write(protocol) must be(())
          }
        }
      }

      "exception" in { _ =>
        Xception(1, "boom").isInstanceOf[Exception] must be(true)
        Xception(1, "boom").isInstanceOf[ThriftException] must be(true)
        Xception(1, "boom").isInstanceOf[SourcedException] must be(true)
        Xception(1, "boom").isInstanceOf[ThriftStruct] must be(true)
        Xception(2, "kathunk").getMessage must be("kathunk")
      }

      "exception getMessage" in { _ =>
        StringMsgException(1, "jeah").getMessage must be("jeah")
        NonStringMessageException(5).getMessage must be("5")
      }

      "with more than 22 fields" should {
        "apply" in { _ =>
          Biggie().num25 must be(25)
        }

        "two default object must be equal" in { _ =>
          Biggie() must be(Biggie())
        }

        "copy and equals" in { _ =>
          Biggie().copy(num10 = -5) must be(Biggie(num10 = -5))
        }

        "hashCode is the same for two similar objects" in { _ =>
          Biggie().hashCode must be(Biggie().hashCode)
          Biggie(num10 = -5).hashCode must be(Biggie(num10 = -5).hashCode)
        }

        "hashCode is different for two different objects" in { _ =>
          Biggie(num10 = -5).hashCode must not be(Biggie().hashCode)
        }

        "toString" in { _ =>
          Biggie().toString must be(("Biggie(" + 1.to(25).map(_.toString).mkString(",") + ")"))
        }
      }

      "unapply single field" in { _ =>
        val struct: Any = RequiredString("hello")
        struct match {
          case RequiredString(value) =>
            value must be("hello")
        }
      }

      "unapply multiple fields" in { _ =>
        val struct: Any = OptionalInt("foo", Some(32))
        struct match {
          case OptionalInt(name, age) =>
            name must be("foo")
            age must be(Some(32))
        }
      }
    }

    "unions" should {
      "have a working apply method" in { _ =>
        val s: String = "bird"
        val cast = Bird.Hummingbird(s)
        Seq(s).map { Bird.Hummingbird.apply }

        val r: Raptor = Raptor(false, "RaptorSpecies")
        val raptorInUnion = Bird.Raptor(r)
        Seq(r).map { Bird.Raptor }
      }

      "zero fields" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            emptyRead(e, protocol)
          }

          whenExecuting {
            intercept[TProtocolException] {
              Bird.decode(protocol)
            }
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          whenExecuting {
            intercept[TProtocolException] {
              Bird.Raptor(null).write(protocol)
            }
          }
        }
      }

      "one field" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString(); will(returnValue("Ruby-Throated"))
            endRead(e, protocol)
          }

          whenExecuting {
            Bird.decode(protocol) must be(Bird.Hummingbird("Ruby-Throated"))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).writeString(`with`(Expectations.equal("foo")))
            endWrite(e, protocol)
          }

          whenExecuting {
            Bird.OwletNightjar("foo").write(protocol)
          }
        }
      }

      "more than one field" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString(); will(returnValue("Anna's Hummingbird"))
            nextRead(e, protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).readBinary(); will(returnValue(ByteBuffer.allocate(1)))
            endRead(e, protocol)
          }

          whenExecuting {
            intercept[TProtocolException] {
              Bird.decode(protocol)
            }
          }
        }

        // no write test because it's not possible
      }

      "nested struct" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("raptor", TType.STRUCT, 1))
            startRead(e, protocol, new TField("isOwl", TType.BOOL, 1))
            one(protocol).readBool(); will(returnValue(false))
            nextRead(e, protocol, new TField("species", TType.STRING, 2))
            one(protocol).readString(); will(returnValue("peregrine"))
            endRead(e, protocol)
            endRead(e, protocol)
          }

          whenExecuting {
            Bird.decode(protocol) must be(Bird.Raptor(Raptor(false, "peregrine")))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("raptor", TType.STRUCT, 1))
            startWrite(e, protocol, new TField("isOwl", TType.BOOL, 1))
            one(protocol).writeBool(`with`(Expectations.equal(true)))
            nextWrite(e, protocol, new TField("species", TType.STRING, 2))
            one(protocol).writeString(`with`(Expectations.equal("Tyto alba")))
            endWrite(e, protocol)
            endWrite(e, protocol)
          }

          whenExecuting {
            Bird.Raptor(Raptor(true, "Tyto alba")).write(protocol)
          }
        }
      }

      "collection" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("flock", TType.LIST, 4))
            one(protocol).readListBegin(); will(returnValue(new TList(TType.STRING, 3)))
            one(protocol).readString(); will(returnValue("starling"))
            one(protocol).readString(); will(returnValue("kestrel"))
            one(protocol).readString(); will(returnValue("warbler"))
            one(protocol).readListEnd()
            endRead(e, protocol)
          }

          whenExecuting {
            Bird.decode(protocol) must be(Bird.Flock(List("starling", "kestrel", "warbler")))
          }
        }

        "write" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startWrite(e, protocol, new TField("flock", TType.LIST, 4))
            one(protocol).writeListBegin(`with`(listEqual(new TList(TType.STRING, 3))))
            one(protocol).writeString(`with`(Expectations.equal("starling")))
            one(protocol).writeString(`with`(Expectations.equal("kestrel")))
            one(protocol).writeString(`with`(Expectations.equal("warbler")))
            one(protocol).writeListEnd()
            endWrite(e, protocol)
          }

          whenExecuting {
            Bird.Flock(List("starling", "kestrel", "warbler")).write(protocol)
          }
        }
      }

      "default value" in { _ =>
        Bird.Hummingbird() must be(Bird.Hummingbird("Calypte anna"))
      }

      "primitive field type" in { _ =>
        import thrift.`def`.default._
        val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
        var original: NaughtyUnion = NaughtyUnion.Value(1)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) must be(original)
        original = NaughtyUnion.Flag(true)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) must be(original)
        original = NaughtyUnion.Text("false")
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) must be(original)
      }
    }

    "typedef relative fields" in { _ =>
      val candy = Candy(100, CandyType.DeliCIous)
      candy.sweetnessIso must be(100)
      candy.candyType.value must be(1)
      candy.brand must be("Hershey")
      candy.count must be(10)
      candy.headline must be("Life is short, eat dessert first")
      candy.defaultCandyType.value must be(0)
    }

    "hide internal helper function to avoid naming conflict" in { _ =>
      import thrift.`def`.default._
      val impl = new NaughtyService[Some] {
        def foo() = Some(FooResult("dummy message"))
      }
      impl.foo().get.message must be("dummy message")
    }

    "passthrough fields" should {
      "passthrough" in { _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())

        val pt1 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        val pt2roundTripped = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough.encode(pt1, protocol)
          PassThrough2.decode(protocol)
        }

        pt2roundTripped must be(pt2)
      }

      "be copied" in { _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())

        val pt1 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        val pt1Copy = pt1.copy(f1 = 2)

        val pt2roundTripped = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough.encode(pt1Copy, protocol)
          PassThrough2.decode(protocol)
        }

        pt2roundTripped must be(PassThrough2(2, PassThroughStruct(), PassThroughStruct()))
      }

      "be removable" in { _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())

        val pt1 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        val pt1f = pt1.unsetField(PassThrough2.F2Field.id)

        val pt2roundTripped = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough.encode(pt1f, protocol)
          PassThrough2.decode(protocol)
        }

        pt2roundTripped must be(PassThrough2(1, null, PassThroughStruct()))
      }

      ".withoutPassthroughFields" in { _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())

        val pt1 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        pt1._passthroughFields.isEmpty must be(false)
        PassThrough.withoutPassthroughFields(pt1)._passthroughFields.isEmpty must be(true)
      }

      ".withoutPassthroughFields recursively" in { _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())
        val pt3 = PassThrough3(pt2)

        val pt4 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough3.encode(pt3, protocol)
          PassThrough4.decode(protocol)
        }

        pt4.f1._passthroughFields.isEmpty must be(false)
        PassThrough4.withoutPassthroughFields(pt4).f1._passthroughFields.isEmpty must be(true)
      }

      ".withoutPassthroughFields recursively with unions" in { _ =>
        val pt5 = PassThrough5(
          PassThroughUnion1.F1(PassThrough2(1, PassThroughStruct(), PassThroughStruct()))
        )

        val pt6 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough5.encode(pt5, protocol)
          PassThrough6.decode(protocol)
        }

        def checkInside(s: PassThrough6) = {
          s.f1.isInstanceOf[PassThroughUnion2.F1]
          val ptu2 = s.f1.asInstanceOf[PassThroughUnion2.F1]
          ptu2.f1._passthroughFields.isEmpty
        }

        checkInside(pt6) must be(false)
        checkInside(PassThrough6.withoutPassthroughFields(pt6)) must be(true)
      }

      "be able to add more" in {  _ =>
        val pt1 = PassThrough(1)
        val pt2 = PassThrough2(1, PassThroughStruct(), null)
        val f2 = pt2.getFieldBlob(PassThrough2.F2Field.id).get
        val pt1w = pt1.setField(f2)

        val pt2roundTripped = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough.encode(pt1w, protocol)
          PassThrough2.decode(protocol)
        }

        pt2roundTripped must be(pt2)
      }

      "be proxy-able" in {  _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())

        val pt1 = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        val proxy = new PassThrough.Proxy {
          val _underlying_PassThrough = pt1
        }

        val pt2roundTripped = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough.encode(proxy, protocol)
          PassThrough2.decode(protocol)
        }

        pt2roundTripped must be(pt2)
      }

      "be equallable" in { _ =>
        val pt2 = PassThrough2(1, PassThroughStruct(), PassThroughStruct())

        val pt1a = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        val pt1b = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough2.encode(pt2, protocol)
          PassThrough.decode(protocol)
        }

        pt1a must be(pt1b)
      }
    }

    "gracefully handle null fields" in { _ =>
      val prot = new TBinaryProtocol(new TMemoryBuffer(256))
      val emp = Emperor(null, 0)

      // basically these shouldn't blow up
      Emperor.encode(emp, prot)
      Emperor.decode(prot) must be(emp)
    }

    "generate with special scala namespace syntax" in { _ =>
      scrooge.test.thriftscala.Thingymabob().isInstanceOf[scrooge.test.thriftscala.Thingymabob] must be(true)
    }

    "generate productElement correctly" in { _ =>
      val struct = thrift.collision.ProductElementStruct(a = "a", n = "n")

      struct.isInstanceOf[scala.Product] must be(true)
      struct.isInstanceOf[scala.Product2[_, _]] must be(true)
      struct.productArity must be(2)

      struct.a must be("a")
      struct._1 must be("a")
      struct.productElement(0) must be("a")

      struct.n must be("n")
      struct._2 must be("n")
      struct.productElement(1) must be("n")
    }
  }
}
