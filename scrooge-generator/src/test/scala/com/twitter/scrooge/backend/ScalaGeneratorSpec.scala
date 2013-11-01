package com.twitter.scrooge.backend

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import org.specs.mock.{ClassMocker, JMocker}
import org.specs.SpecificationWithJUnit
import com.twitter.finagle.SourcedException
import com.twitter.scrooge.testutil.EvalHelper
import com.twitter.scrooge.{ThriftStruct, ThriftException}
import thrift.test._
import thrift.test1._
import thrift.test2._
import thrift.`def`.default._

class ScalaGeneratorSpec extends SpecificationWithJUnit with EvalHelper with JMocker with ClassMocker {
  val protocol = mock[TProtocol]

  def stringToBytes(string: String) = ByteBuffer.wrap(string.getBytes)

  "ScalaGenerator" should {
    "generate an enum" in {
      "correct constants" in {
        NumberID.One.getValue mustEqual 1
        NumberID.Two.getValue mustEqual 2
        NumberID.Three.getValue mustEqual 3
        NumberID.Five.getValue mustEqual 5
        NumberID.Six.getValue mustEqual 6
        NumberID.Eight.getValue mustEqual 8
      }

      "correct names" in {
        NumberID.One.name mustEqual "One"
        NumberID.Two.name mustEqual "Two"
        NumberID.Three.name mustEqual "Three"
        NumberID.Five.name mustEqual "Five"
        NumberID.Six.name mustEqual "Six"
        NumberID.Eight.name mustEqual "Eight"
      }

      "apply" in {
        NumberID(1) mustEqual NumberID.One
        NumberID(2) mustEqual NumberID.Two
        NumberID(3) mustEqual NumberID.Three
        NumberID(5) mustEqual NumberID.Five
        NumberID(6) mustEqual NumberID.Six
        NumberID(8) mustEqual NumberID.Eight
      }

      "get" in {
        NumberID.get(1) must beSome(NumberID.One)
        NumberID.get(2) must beSome(NumberID.Two)
        NumberID.get(3) must beSome(NumberID.Three)
        NumberID.get(5) must beSome(NumberID.Five)
        NumberID.get(6) must beSome(NumberID.Six)
        NumberID.get(8) must beSome(NumberID.Eight)
        NumberID.get(10) must beNone
      }

      "valueOf" in {
        NumberID.valueOf("One") must beSome(NumberID.One)
        NumberID.valueOf("Two") must beSome(NumberID.Two)
        NumberID.valueOf("Three") must beSome(NumberID.Three)
        NumberID.valueOf("Five") must beSome(NumberID.Five)
        NumberID.valueOf("Six") must beSome(NumberID.Six)
        NumberID.valueOf("Eight") must beSome(NumberID.Eight)
        NumberID.valueOf("Ten") must beNone
      }

      "correct list" in {
        NumberID.list(0) mustEqual NumberID.One
        NumberID.list(1) mustEqual NumberID.Two
        NumberID.list(2) mustEqual NumberID.Three
        NumberID.list(3) mustEqual NumberID.Five
        NumberID.list(4) mustEqual NumberID.Six
        NumberID.list(5) mustEqual NumberID.Eight
        NumberID.list.size mustEqual 6
      }

      "java-serializable" in {
        val bos = new ByteArrayOutputStream()
        val out = new ObjectOutputStream(bos)
        out.writeObject(NumberID.One)
        out.writeObject(NumberID.Two)
        bos.close()
        val bytes = bos.toByteArray

        val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
        var obj = in.readObject()
        obj.isInstanceOf[NumberID] must beTrue
        obj.asInstanceOf[NumberID].getValue mustEqual NumberID.One.getValue
        obj.asInstanceOf[NumberID].name mustEqual NumberID.One.name

        obj = in.readObject()
        obj.isInstanceOf[NumberID] must beTrue
        obj.asInstanceOf[NumberID].getValue mustEqual NumberID.Two.getValue
        obj.asInstanceOf[NumberID].name mustEqual NumberID.Two.name
      }

      "handle namespace collisions" in {
        NamespaceCollisions.List.name mustEqual "List"
        NamespaceCollisions.Any.name mustEqual "Any"
        NamespaceCollisions.AnyRef.name mustEqual "AnyRef"
        NamespaceCollisions.Object.name mustEqual "Object"
        NamespaceCollisions.String.name mustEqual "String"
        NamespaceCollisions.Byte.name mustEqual "Byte"
        NamespaceCollisions.Short.name mustEqual "Short"
        NamespaceCollisions.Char.name mustEqual "Char"
        NamespaceCollisions.Int.name mustEqual "Int"
        NamespaceCollisions.Long.name mustEqual "Long"
        NamespaceCollisions.Float.name mustEqual "Float"
        NamespaceCollisions.Double.name mustEqual "Double"
        NamespaceCollisions.Option.name mustEqual "Option"
        NamespaceCollisions.None.name mustEqual "None"
        NamespaceCollisions.Some.name mustEqual "Some"
        NamespaceCollisions.Nil.name mustEqual "Nil"
        NamespaceCollisions.Null.name mustEqual "Null"
        NamespaceCollisions.Set.name mustEqual "Set"
        NamespaceCollisions.Map.name mustEqual "Map"
        NamespaceCollisions.Seq.name mustEqual "Seq"
        NamespaceCollisions.Array.name mustEqual "Array"
        NamespaceCollisions.Iterable.name mustEqual "Iterable"
        NamespaceCollisions.Unit.name mustEqual "Unit"
        NamespaceCollisions.Nothing.name mustEqual "Nothing"
        NamespaceCollisions.Protected.name mustEqual "Protected"

        NamespaceCollisions.valueOf("null") must beSome(NamespaceCollisions.Null)
        NamespaceCollisions.valueOf("protected") must beSome(NamespaceCollisions.Protected)
      }

      "encode-decode in struct" in {
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val eStruct = EnumStruct(NumberID.One)
        EnumStruct.encode(eStruct, prot)
        EnumStruct.decode(prot) mustEqual eStruct
      }

      "encode-decode in union" in {
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val eUnion = EnumUnion.Number(NumberID.One)
        EnumUnion.encode(eUnion, prot)
        EnumUnion.decode(prot) mustEqual eUnion
      }

      "be identified as an ENUM" in {
        EnumStruct.NumberField.`type` mustEqual TType.ENUM
      }

      "be identified as an I32 on the wire for structs" in {
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        EnumStruct.encode(EnumStruct(NumberID.One), prot)
        prot.readStructBegin()
        val field = prot.readFieldBegin()
        field.`type` mustEqual TType.I32
      }

      "be identified as an I32 on the wire for unions" in {
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        EnumUnion.encode(EnumUnion.Number(NumberID.One), prot)
        prot.readStructBegin()
        val field = prot.readFieldBegin()
        field.`type` mustEqual TType.I32
      }

      "be identified as I32 on the wire for collections" in {
        val prot = new TBinaryProtocol(new TMemoryBuffer(64))
        val eStruct = EnumCollections(
          aMap = Map(NumberID.One -> NumberID.Two),
          aList = List(NumberID.One),
          aSet = Set(NumberID.One))

        EnumCollections.encode(eStruct, prot)
        EnumCollections.decode(prot) mustEqual eStruct

        EnumCollections.encode(eStruct, prot)

        prot.readStructBegin()

        // Test Map encoding
        prot.readFieldBegin()
        val mapField = prot.readMapBegin()
        mapField.keyType mustEqual TType.I32
        mapField.valueType mustEqual TType.I32
        prot.readI32(); prot.readI32()
        prot.readMapEnd()
        prot.readFieldEnd()

        // Test List encoding
        prot.readFieldBegin()
        val listField = prot.readListBegin()
        listField.elemType mustEqual TType.I32
        prot.readI32()
        prot.readListEnd()
        prot.readFieldEnd()

        // Test Set encoding
        prot.readFieldBegin()
        val setField = prot.readSetBegin()
        setField.elemType mustEqual TType.I32
      }
    }

    "generate constants" in {
      thrift.test.Constants.myWfhDay mustEqual WeekDay.Thu
      thrift.test.Constants.myDaysOut mustEqual List(WeekDay.Thu, WeekDay.Sat, WeekDay.SUN)
      thrift.test.Constants.name mustEqual "Columbo"
      thrift.test.Constants.someInt mustEqual 1
      thrift.test.Constants.someDouble mustEqual 3.0
      thrift.test.Constants.someList mustEqual List("piggy")
      thrift.test.Constants.emptyList mustEqual List()
      thrift.test.Constants.someMap mustEqual Map("foo" -> "bar")
      thrift.test.Constants.someSimpleSet mustEqual Set("foo", "bar")
      thrift.test.Constants.someSet mustEqual Set(
        List("piggy"),
        List("kitty")
      )
    }

    "basic structs" in {
      "ints" in {
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

          Ints.decode(protocol) mustEqual Ints(16, 32, 64L)
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

          Ints(16, 32, 64L).write(protocol) mustEqual ()
        }
      }

      "bytes" in {
        "read" in {
          expect {
            startRead(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).readByte() willReturn 3.toByte
            nextRead(protocol, new TField("y", TType.STRING, 2))
            one(protocol).readBinary() willReturn stringToBytes("hello")
            endRead(protocol)
          }

          val bytes = Bytes.decode(protocol)
          bytes.x mustEqual 3.toByte
          new String(bytes.y.array) mustEqual "hello"
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).writeByte(16.toByte)
            nextWrite(protocol, new TField("y", TType.STRING, 2))
            one(protocol).writeBinary(stringToBytes("goodbye"))
            endWrite(protocol)
          }

          Bytes(16.toByte, stringToBytes("goodbye")).write(protocol) mustEqual ()
        }
      }

      "bool, double, string" in {
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

          Misc.decode(protocol) mustEqual Misc(true, 3.14, "bender")
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

          Misc(false, 6.28, "fry").write(protocol) mustEqual ()
        }
      }

      "lists, sets, and maps" in {
        val exemplar = Compound(
          intlist = List(10, 20),
          intset = Set(44, 55),
          namemap = Map("wendy" -> 500),
          nested = List(Set(9)))

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

          Compound.decode(protocol) mustEqual exemplar
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

          exemplar.write(protocol) mustEqual ()
        }
      }
    }

    "complicated structs" in {
      "with required fields" in {
        "read" in {
          expect {
            startRead(protocol, new TField("string", TType.STRING, 1))
            one(protocol).readString() willReturn "yo"
            endRead(protocol)
          }

          RequiredString.decode(protocol) mustEqual RequiredString("yo")
        }

        "missing required value throws exception during deserialization" in {
          doBefore {
            expect {
              emptyRead(protocol)
            }
          }

          "with no default value" in {
            RequiredString.decode(protocol) must throwA[TProtocolException]
          }

          "with default value" in {
            RequiredStringWithDefault.decode(protocol) must throwA[TProtocolException]
          }
        }

        "null required value throws exception during serialization" in {
          "with no default value" in {
            RequiredString(value = null).write(protocol) must throwA[TProtocolException]
          }

          "with default value" in {
            RequiredStringWithDefault(value = null).write(protocol) must throwA[TProtocolException]
          }
        }
      }

      "with optional fields" in {
        "read" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            nextRead(protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32() willReturn 14
            endRead(protocol)
          }

          OptionalInt.decode(protocol) mustEqual OptionalInt("Commie", Some(14))
        }

        "read with missing field" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            endRead(protocol)
          }

          OptionalInt.decode(protocol) mustEqual OptionalInt("Commie", None)
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            nextWrite(protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(14)
            endWrite(protocol)
          }

          OptionalInt("Commie", Some(14)).write(protocol) mustEqual ()
        }

        "write with missing field" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            endWrite(protocol)
          }

          OptionalInt("Commie", None).write(protocol) mustEqual ()
        }
      }

      "with default values" in {
        "read with value missing, using default" in {
          expect {
            one(protocol).readStructBegin()
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues.decode(protocol) mustEqual DefaultValues("leela")
        }

        "read with value present" in {
          expect {
            one(protocol).readStructBegin()
            nextRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "delilah"
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues.decode(protocol) mustEqual DefaultValues("delilah")
        }
      }

      "nested" in {
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

          Empire.decode(protocol) mustEqual Empire(
            "United States of America",
            List("connecticut", "california"),
            Emperor("Bush", 42))
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

          Empire(
            "Canada",
            List("Manitoba", "Alberta"),
            Emperor("Larry", 13)
          ).write(protocol) mustEqual ()
        }
      }

      "exception" in {
        Xception(1, "boom") must haveSuperClass[Exception]
        Xception(1, "boom") must haveSuperClass[ThriftException]
        Xception(1, "boom") must haveSuperClass[SourcedException]
        Xception(1, "boom") must haveSuperClass[ThriftStruct]
        Xception(2, "kathunk").getMessage mustEqual "kathunk"
      }

      "exception getMessage" in {
        StringMsgException(1, "jeah").getMessage mustEqual "jeah"
        NonStringMessageException(5).getMessage mustEqual "5"
      }

      "with more than 22 fields" in {
        "apply" in {
          Biggie().num25 mustEqual 25
        }

        "two default object must be equal" in {
          Biggie() mustEqual Biggie()
        }

        "copy and equals" in {
          Biggie().copy(num10 = -5) mustEqual Biggie(num10 = -5)
        }

        "hashCode is the same for two similar objects" in {
          Biggie().hashCode mustEqual Biggie().hashCode
          Biggie(num10 = -5).hashCode mustEqual Biggie(num10 = -5).hashCode
        }

        "hashCode is different for two different objects" in {
          Biggie(num10 = -5).hashCode mustNot beEqual(Biggie().hashCode)
        }

        "toString" in {
          Biggie().toString mustEqual ("Biggie(" + 1.to(25).map(_.toString).mkString(",") + ")")
        }
      }

      "unapply single field" in {
        val struct: Any = RequiredString("hello")
        struct match {
          case RequiredString(value) =>
            value mustEqual "hello"
        }
      }

      "unapply multiple fields" in {
        val struct: Any = OptionalInt("foo", Some(32))
        struct match {
          case OptionalInt(name, age) =>
            name mustEqual "foo"
            age must beSome(32)
        }
      }
    }

    "unions" in {
      "zero fields" in {
        "read" in {
          expect {
            emptyRead(protocol)
          }

          Bird.decode(protocol) must throwA[TProtocolException]
        }

        "write" in {
          Bird.Raptor(null).write(protocol) must throwA[TProtocolException]
        }
      }

      "one field" in {
        "read" in {
          expect {
            startRead(protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString() willReturn "Ruby-Throated"
            endRead(protocol)
          }

          Bird.decode(protocol) mustEqual Bird.Hummingbird("Ruby-Throated")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).writeString("foo")
            endWrite(protocol)
          }

          Bird.OwletNightjar("foo").write(protocol)
        }
      }

      "more than one field" in {
        "read" in {
          expect {
            startRead(protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString() willReturn "Anna's Hummingbird"
            nextRead(protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).readBinary() willReturn ByteBuffer.allocate(1)
            endRead(protocol)
          }

          Bird.decode(protocol) must throwA[TProtocolException]
        }

        // no write test because it's not possible
      }

      "nested struct" in {
        "read" in {
          expect {
            startRead(protocol, new TField("raptor", TType.STRUCT, 1))
            startRead(protocol, new TField("isOwl", TType.BOOL, 1))
            one(protocol).readBool() willReturn false
            nextRead(protocol, new TField("species", TType.STRING, 2))
            one(protocol).readString() willReturn "peregrine"
            endRead(protocol)
            endRead(protocol)
          }

          Bird.decode(protocol) mustEqual Bird.Raptor(Raptor(false, "peregrine"))
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("raptor", TType.STRUCT, 1))
            startWrite(protocol, new TField("isOwl", TType.BOOL, 1))
            one(protocol).writeBool(true)
            nextWrite(protocol, new TField("species", TType.STRING, 2))
            one(protocol).writeString("Tyto alba")
            endWrite(protocol)
            endWrite(protocol)
          }

          Bird.Raptor(Raptor(true, "Tyto alba")).write(protocol)
        }
      }

      "collection" in {
        "read" in {
          expect {
            startRead(protocol, new TField("flock", TType.LIST, 4))
            one(protocol).readListBegin() willReturn new TList(TType.STRING, 3)
            one(protocol).readString() willReturn "starling"
            one(protocol).readString() willReturn "kestrel"
            one(protocol).readString() willReturn "warbler"
            one(protocol).readListEnd()
            endRead(protocol)
          }

          Bird.decode(protocol) mustEqual Bird.Flock(List("starling", "kestrel", "warbler"))
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("flock", TType.LIST, 4))
            one(protocol).writeListBegin(equal(new TList(TType.STRING, 3)))
            one(protocol).writeString("starling")
            one(protocol).writeString("kestrel")
            one(protocol).writeString("warbler")
            one(protocol).writeListEnd()
            endWrite(protocol)
          }

          Bird.Flock(List("starling", "kestrel", "warbler")).write(protocol)
        }
      }

      "default value" in {
        Bird.Hummingbird() mustEqual Bird.Hummingbird("Calypte anna")
      }

      "primitive field type" in {
        import thrift.`def`.default._
        val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
        var original: NaughtyUnion = NaughtyUnion.Value(1)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) mustEqual(original)
        original = NaughtyUnion.Flag(true)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) mustEqual(original)
        original = NaughtyUnion.Text("false")
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) mustEqual(original)
      }
    }

    "typedef relative fields" in {
      val candy = Candy(100, CandyType.DeliCIous)
      candy.sweetnessIso mustEqual 100
      candy.candyType.value mustEqual 1
      candy.brand mustEqual "Hershey"
      candy.count mustEqual 10
      candy.headline mustEqual "Life is short, eat dessert first"
    }

    "hide internal helper function to avoid naming conflict" in {
      import thrift.`def`.default._
      val impl = new NaughtyService[Some] {
        def foo() = Some(FooResult("dummy message"))
      }
      impl.foo().get.message mustEqual("dummy message")
    }

    "pass through fields" in {
      "pass through" in {
        val pt2 = PassThrough2(1, 2)

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

        pt2roundTripped mustEqual pt2
      }

      "be copied" in {
        val pt2 = PassThrough2(1, 2)

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

        pt2roundTripped mustEqual PassThrough2(2, 2)
      }

      "be removable" in {
        val pt2 = PassThrough2(1, 2)

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

        pt2roundTripped mustEqual PassThrough2(1, 0)
      }

      "be able to add more" in {
        val pt1 = PassThrough(1)
        val pt2 = PassThrough2(1, 2)
        val f2 = pt2.getFieldBlob(PassThrough2.F2Field.id).get
        val pt1w = pt1.setField(f2)

        val pt2roundTripped = {
          val protocol = new TBinaryProtocol(new TMemoryBuffer(256))
          PassThrough.encode(pt1w, protocol)
          PassThrough2.decode(protocol)
        }

        pt2roundTripped mustEqual pt2
      }

      "be proxy-able" in {
        val pt2 = PassThrough2(1, 2)

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

        pt2roundTripped mustEqual pt2
      }

      "be equallable" in {
        val pt2 = PassThrough2(1, 2)

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

        pt1a mustEqual pt1b
      }
    }

    "gracefully handle null fields" in {
      val prot = new TBinaryProtocol(new TMemoryBuffer(256))
      val emp = Emperor(null, 0)

      // basically these shouldn't blow up
      Emperor.encode(emp, prot)
      Emperor.decode(prot) mustEqual emp
    }

    "generate with special scala namespace syntax" in {
      scrooge.test.thriftscala.Thingymabob() must haveSuperClass[scrooge.test.thriftscala.Thingymabob]
    }
  }
}
