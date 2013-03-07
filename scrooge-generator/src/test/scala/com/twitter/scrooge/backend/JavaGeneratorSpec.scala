package com.twitter.scrooge
package backend

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import org.specs.mock.{ClassMocker, JMocker}
import org.specs.SpecificationWithJUnit
import com.twitter.scrooge.testutil.EvalHelper
import thrift.java_test._

class JavaGeneratorSpec extends SpecificationWithJUnit with EvalHelper with JMocker with ClassMocker {
  val protocol = mock[TProtocol]

  def stringToBytes(string: String) = ByteBuffer.wrap(string.getBytes)

  "JavaGenerator" should {
    "generate an enum" in {
      "correct constants" in {
        NumberID.ONE.getValue() mustEqual 1
        NumberID.TWO.getValue() mustEqual 2
        NumberID.THREE.getValue() mustEqual 3
        NumberID.FIVE.getValue() mustEqual 5
        NumberID.SIX.getValue() mustEqual 6
        NumberID.EIGHT.getValue() mustEqual 8
      }

      "findByValue" in {
        NumberID.findByValue(1) mustEqual NumberID.ONE
        NumberID.findByValue(2) mustEqual NumberID.TWO
        NumberID.findByValue(3) mustEqual NumberID.THREE
        NumberID.findByValue(5) mustEqual NumberID.FIVE
        NumberID.findByValue(6) mustEqual NumberID.SIX
        NumberID.findByValue(8) mustEqual NumberID.EIGHT
      }

      "java-serializable" in {
        val bos = new ByteArrayOutputStream()
        val out = new ObjectOutputStream(bos)
        out.writeObject(NumberID.ONE)
        out.writeObject(NumberID.TWO)
        bos.close()
        val bytes = bos.toByteArray

        val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
        var obj = in.readObject()
        obj.isInstanceOf[NumberID] must beTrue
        obj.asInstanceOf[NumberID].getValue mustEqual NumberID.ONE.getValue
        obj.asInstanceOf[NumberID].name mustEqual NumberID.ONE.name
        obj = in.readObject()
        obj.isInstanceOf[NumberID] must beTrue
        obj.asInstanceOf[NumberID].getValue mustEqual NumberID.TWO.getValue
        obj.asInstanceOf[NumberID].name mustEqual NumberID.TWO.name
      }
    }

    "generate constants" in {
      Constants.myWfhDay mustEqual WeekDay.THU
      Constants.myDaysOut mustEqual Utilities.makeList(WeekDay.THU, WeekDay.SAT, WeekDay.SUN)
      Constants.name mustEqual "Columbo"
      Constants.someInt mustEqual 1
      Constants.someDouble mustEqual 3.0
      Constants.someList mustEqual Utilities.makeList("piggy")
      Constants.emptyList mustEqual Utilities.makeList()
      Constants.someMap mustEqual Utilities.makeMap(Utilities.makeTuple("foo", "bar"))
      Constants.someSimpleSet mustEqual Utilities.makeSet("foo", "bar")
      Constants.someSet mustEqual Utilities.makeSet(
        Utilities.makeList("piggy"),
        Utilities.makeList("kitty")
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

          Ints.decode(protocol) mustEqual new Ints(16, 32, 64L)
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

          new Ints(16, 32, 64L).write(protocol) mustEqual ()
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
          bytes.getX mustEqual 3.toByte
          new String(bytes.getY.array) mustEqual "hello"
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("x", TType.BYTE, 1))
            one(protocol).writeByte(16.toByte)
            nextWrite(protocol, new TField("y", TType.STRING, 2))
            one(protocol).writeBinary(stringToBytes("goodbye"))
            endWrite(protocol)
          }

          new Bytes(16.toByte, stringToBytes("goodbye")).write(protocol) mustEqual ()
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

          Misc.decode(protocol) mustEqual new Misc(true, 3.14, "bender")
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

          new Misc(false, 6.28, "fry").write(protocol) mustEqual ()
        }
      }

      "lists, sets, and maps" in {
        val exemplar = new Compound.Builder()
          .intlist(Utilities.makeList(10, 20))
          .intset(Utilities.makeSet(44, 55))
          .namemap(Utilities.makeMap(Utilities.makeTuple("wendy", 500)))
          .nested(Utilities.makeList(Utilities.makeSet(9)))
        .build()

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

          RequiredString.decode(protocol) mustEqual new RequiredString("yo")
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
            new RequiredString(null).write(protocol) must throwA[TProtocolException]
          }

          "with default value" in {
            new RequiredStringWithDefault(null).write(protocol) must throwA[TProtocolException]
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

          OptionalInt.decode(protocol) mustEqual new OptionalInt("Commie", new Option.Some(14))
        }

        "read with missing field" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            endRead(protocol)
          }

          OptionalInt.decode(protocol) mustEqual new OptionalInt("Commie", Option.none())
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            nextWrite(protocol, new TField("age", TType.I32, 2))
            one(protocol).writeI32(14)
            endWrite(protocol)
          }

          new OptionalInt("Commie", new Option.Some(14)).write(protocol) mustEqual ()
        }

        "write with missing field" in {
          expect {
            startWrite(protocol, new TField("name", TType.STRING, 1))
            one(protocol).writeString("Commie")
            endWrite(protocol)
          }

          new OptionalInt("Commie", Option.none()).write(protocol) mustEqual ()
        }
      }

      "with default values" in {
        "read with value missing, using default" in {
          expect {
            one(protocol).readStructBegin()
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues.decode(protocol) mustEqual new DefaultValues("leela")
        }

        "read with value present" in {
          expect {
            one(protocol).readStructBegin()
            nextRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "delilah"
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues.decode(protocol) mustEqual new DefaultValues("delilah")
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

          Empire.decode(protocol) mustEqual new Empire(
            "United States of America",
            Utilities.makeList("connecticut", "california"),
            new Emperor("Bush", 42))
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

          new Empire(
            "Canada",
            Utilities.makeList("Manitoba", "Alberta"),
            new Emperor("Larry", 13)
          ).write(protocol) mustEqual ()
        }
      }

      "exception" in {
        new Xception(1, "boom") must haveSuperClass[Exception]
        new Xception(2, "kathunk").getMessage mustEqual "kathunk"
      }

      "exception getMessage" in {
        new StringMsgException(1, "jeah").getMessage mustEqual "jeah"
        new NonStringMessageException(5).getMessage mustEqual "5"
      }

      "with more than 22 fields" in {
        "apply" in {
          new Biggie.Builder().build().getNum25() mustEqual 25
        }

        "two default object must be equal" in {
          new Biggie.Builder().build() mustEqual new Biggie.Builder().build()
        }

        "copy and equals" in {
          new Biggie.Builder().build().copy().num10(-5).build() mustEqual new Biggie.Builder().num10(-5).build()
        }

        "hashCode is the same for two similar objects" in {
          new Biggie.Builder().build().hashCode mustEqual new Biggie.Builder().build().hashCode
          new Biggie.Builder().num10(-5).build().hashCode mustEqual new Biggie.Builder().num10(-5).build().hashCode
        }

        "hashCode is different for two different objects" in {
          new Biggie.Builder().num10(-5).build().hashCode mustNot beEqual(new Biggie.Builder().build().hashCode)
        }

        "toString" in {
          new Biggie.Builder().build().toString mustEqual ("Biggie(" + 1.to(25).map(_.toString).mkString(",") + ")")
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

        "instantiate" in {
          Bird.newRaptor(null) must throwA[NullPointerException]
        }
      }

      "one field" in {
        "read" in {
          expect {
            startRead(protocol, new TField("hummingbird", TType.STRING, 2))
            one(protocol).readString() willReturn "Ruby-Throated"
            endRead(protocol)
          }

          Bird.decode(protocol) mustEqual Bird.newHummingbird("Ruby-Throated")
        }

        "write" in {
          expect {
            startWrite(protocol, new TField("owlet_nightjar", TType.STRING, 3))
            one(protocol).writeString("foo")
            endWrite(protocol)
          }

          Bird.newOwletNightjar("foo").write(protocol)
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

          Bird.decode(protocol) mustEqual Bird.newRaptor(new Raptor(false, "peregrine"))
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

          Bird.newRaptor(new Raptor(true, "Tyto alba")).write(protocol)
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

          Bird.decode(protocol) mustEqual Bird.newFlock(Utilities.makeList("starling", "kestrel", "warbler"))
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

          Bird.newFlock(Utilities.makeList("starling", "kestrel", "warbler")).write(protocol)
        }
      }

      "primitive field type" in {
        import thrift.java_def._default_._
        val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
        var original: NaughtyUnion = NaughtyUnion.newValue(1)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) mustEqual(original)
        original = NaughtyUnion.newFlag(true)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) mustEqual(original)
        original = NaughtyUnion.newText("false")
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) mustEqual(original)
      }
    }
  }
}
