package com.twitter.scrooge.backend

import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.specs.mock.{ClassMocker, JMocker}
import org.specs.SpecificationWithJUnit
import thrift.test._
import thrift.test1._
import thrift.test2._
import com.twitter.scrooge.EvalHelper

//import com.twitter.scrooge.ast._

class ScalaGeneratorSpec extends SpecificationWithJUnit with EvalHelper with JMocker with ClassMocker {
  val protocol = mock[TProtocol]

  def stringToBytes(string: String) = ByteBuffer.wrap(string.getBytes)

  "ScalaGenerator" should {
    "generate an enum" in {
      "correct constants" in {
        Numberz.One.value mustEqual 1
        Numberz.Two.value mustEqual 2
        Numberz.Three.value mustEqual 3
        Numberz.Five.value mustEqual 5
        Numberz.Six.value mustEqual 6
        Numberz.Eight.value mustEqual 8
      }

      "apply" in {
        Numberz(1) mustEqual Numberz.One
        Numberz(2) mustEqual Numberz.Two
        Numberz(3) mustEqual Numberz.Three
        Numberz(5) mustEqual Numberz.Five
        Numberz(6) mustEqual Numberz.Six
        Numberz(8) mustEqual Numberz.Eight
      }

      "get" in {
        Numberz.get(1) must beSome(Numberz.One)
        Numberz.get(2) must beSome(Numberz.Two)
        Numberz.get(3) must beSome(Numberz.Three)
        Numberz.get(5) must beSome(Numberz.Five)
        Numberz.get(6) must beSome(Numberz.Six)
        Numberz.get(8) must beSome(Numberz.Eight)
        Numberz.get(10) must beNone
      }

      "valueOf" in {
        Numberz.valueOf("One") must beSome(Numberz.One)
        Numberz.valueOf("Two") must beSome(Numberz.Two)
        Numberz.valueOf("Three") must beSome(Numberz.Three)
        Numberz.valueOf("Five") must beSome(Numberz.Five)
        Numberz.valueOf("Six") must beSome(Numberz.Six)
        Numberz.valueOf("Eight") must beSome(Numberz.Eight)
        Numberz.valueOf("Ten") must beNone
      }
    }

    "generate constants" in {
      thrift.test.Constants.myNumberz mustEqual Numberz.One
      thrift.test.Constants.name mustEqual "Columbo"
      thrift.test.Constants.someInt mustEqual 1
      thrift.test.Constants.someDouble mustEqual 3.0
      thrift.test.Constants.someList mustEqual List("piggy")
      thrift.test.Constants.someMap mustEqual Map("foo" -> "bar")
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

          Ints(protocol) mustEqual Ints(16, 32, 64L)
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

          val bytes = Bytes(protocol)
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

          Misc(protocol) mustEqual Misc(true, 3.14, "bender")
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

          Compound(protocol) mustEqual exemplar
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

          RequiredString(protocol) mustEqual RequiredString("yo")
        }

        "missing required value throws exception during deserialization" in {
          doBefore {
            expect {
              emptyRead(protocol)
            }
          }

          "with no default value" in {
            RequiredString(protocol) must throwA[TProtocolException]
          }

          "with default value" in {
            RequiredStringWithDefault(protocol) must throwA[TProtocolException]
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

          OptionalInt(protocol) mustEqual OptionalInt("Commie", Some(14))
        }

        "read with missing field" in {
          expect {
            startRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "Commie"
            endRead(protocol)
          }

          OptionalInt(protocol) mustEqual OptionalInt("Commie", None)
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

          DefaultValues(protocol) mustEqual DefaultValues("leela")
        }

        "read with value present" in {
          expect {
            one(protocol).readStructBegin()
            nextRead(protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString() willReturn "delilah"
            one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
            one(protocol).readStructEnd()
          }

          DefaultValues(protocol) mustEqual DefaultValues("delilah")
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

          Empire(protocol) mustEqual Empire(
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
        Xception(2, "kathunk").getMessage mustEqual "kathunk"
      }

      "exception getMessage" in {
        StringMsgException(1, "jeah").getMessage mustEqual "jeah"
        NonStringMessageException(5).getMessage mustEqual "5"
      }

      "funky names that scala doesn't like" in {
        Naughty("car", 100).`type` mustEqual "car"
        Naughty("car", 100).`def` mustEqual 100
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

    "typedef relative fields" in {
      val candy = Candy(100, CandyType.Delicious)
      candy.sweetnessIso mustEqual 100
      candy.candyType.value mustEqual 1
      candy.brand mustEqual "Hershey"
      candy.count mustEqual 10
      candy.headline mustEqual "Life is short, eat dessert first"
    }
  }
}
