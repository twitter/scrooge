package com.twitter.scrooge
package backend

import com.twitter.scrooge.testutil.{EvalHelper, JMockSpec}
import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import org.jmock.Expectations
import org.jmock.Expectations.{any, returnValue}
import thrift.java_test._

class JavaGeneratorSpec extends JMockSpec with EvalHelper {
  def stringToBytes(string: String) = ByteBuffer.wrap(string.getBytes)

  "JavaGenerator" should {
    "generate an enum" should {
      "correct constants" in { _ =>
        NumberID.ONE.getValue() must be(1)
        NumberID.TWO.getValue() must be(2)
        NumberID.THREE.getValue() must be(3)
        NumberID.FIVE.getValue() must be(5)
        NumberID.SIX.getValue() must be(6)
        NumberID.EIGHT.getValue() must be(8)
      }

      "findByValue" in { _ =>
        NumberID.findByValue(1) must be(NumberID.ONE)
        NumberID.findByValue(2) must be(NumberID.TWO)
        NumberID.findByValue(3) must be(NumberID.THREE)
        NumberID.findByValue(5) must be(NumberID.FIVE)
        NumberID.findByValue(6) must be(NumberID.SIX)
        NumberID.findByValue(8) must be(NumberID.EIGHT)
      }

      "java-serializable" in { _ =>
        val bos = new ByteArrayOutputStream()
        val out = new ObjectOutputStream(bos)
        out.writeObject(NumberID.ONE)
        out.writeObject(NumberID.TWO)
        bos.close()
        val bytes = bos.toByteArray

        val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
        var obj = in.readObject()
        obj.isInstanceOf[NumberID] must be(true)
        obj.asInstanceOf[NumberID].getValue must be(NumberID.ONE.getValue)
        obj.asInstanceOf[NumberID].name must be(NumberID.ONE.name)
        obj = in.readObject()
        obj.isInstanceOf[NumberID] must be(true)
        obj.asInstanceOf[NumberID].getValue must be(NumberID.TWO.getValue)
        obj.asInstanceOf[NumberID].name must be(NumberID.TWO.name)
      }
    }

    "generate constants" in { _ =>
      Constants.myWfhDay must be(WeekDay.THU)
      Constants.myDaysOut must be(Utilities.makeList(WeekDay.THU, WeekDay.SAT, WeekDay.SUN))
      Constants.name must be("Columbo")
      Constants.someInt must be(1)
      Constants.someDouble must be(3.0)
      Constants.someList must be(Utilities.makeList("piggy"))
      Constants.emptyList must be(Utilities.makeList())
      Constants.someMap must be(Utilities.makeMap(Utilities.makeTuple("foo", "bar")))
      Constants.someSimpleSet must be(Utilities.makeSet("foo", "bar"))
      Constants.someSet must be(Utilities.makeSet(
        Utilities.makeList("piggy"),
        Utilities.makeList("kitty")
      ))
    }

    "basic structs" should {
      "ints" should {
        "read" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("baby", TType.I16, 1))
            one(protocol).readI16(); will(returnValue(16: Short))
            nextRead(e, protocol, new TField("mama", TType.I32, 2))
            one(protocol).readI32(); will(returnValue(32))
            nextRead(e, protocol, new TField("papa", TType.I64, 3))
            one(protocol).readI64(); will(returnValue(64L))
            endRead(e, protocol)
          }

          whenExecuting {
            Ints.decode(protocol) must be(new Ints(16, 32, 64L))
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
            new Ints(16, 32, 64L).write(protocol) must be(())
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
            bytes.getX must be(3.toByte)
            new String(bytes.getY.array) must be("hello")
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
            new Bytes(16.toByte, stringToBytes("goodbye")).write(protocol) must be(())
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
            Misc.decode(protocol) must be(new Misc(true, 3.14, "bender"))
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
            new Misc(false, 6.28, "fry").write(protocol) must be(())
          }
        }
      }

      "lists, sets, and maps" should {
        val exemplar = new Compound.Builder()
          .intlist(Utilities.makeList(10, 20))
          .intset(Utilities.makeSet(44, 55))
          .namemap(Utilities.makeMap(Utilities.makeTuple("wendy", 500)))
          .nested(Utilities.makeList(Utilities.makeSet(9)))
        .build()

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
            one(protocol).writeI32(9)
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
            RequiredString.decode(protocol) must be(new RequiredString("yo"))
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
          "with no default value" in { e => import e._
            val protocol = mock[TProtocol]

            intercept[TProtocolException] {
              new RequiredString(null).write(protocol)
            }
          }

          "with default value" in { e => import e._
            val protocol = mock[TProtocol]

            intercept[TProtocolException] {
              new RequiredStringWithDefault(null).write(protocol)
            }
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
            OptionalInt.decode(protocol) must be(new OptionalInt("Commie", new Option.Some(14)))
          }
        }

        "read with missing field" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            startRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue( "Commie"))
            endRead(e, protocol)
          }

          whenExecuting {
            OptionalInt.decode(protocol) must be(new OptionalInt("Commie", Option.none()))
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
            new OptionalInt("Commie", new Option.Some(14)).write(protocol) must be(())
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
            new OptionalInt("Commie", Option.none()).write(protocol) must be(())
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
            DefaultValues.decode(protocol) must be(new DefaultValues("leela"))
          }
        }

        "read with value present" in { cycle => import cycle._
          val protocol = mock[TProtocol]
          expecting { e => import e._
            one(protocol).readStructBegin()
            nextRead(e, protocol, new TField("name", TType.STRING, 1))
            one(protocol).readString(); will(returnValue( "delilah"))
            one(protocol).readFieldBegin(); will(returnValue(new TField("stop", TType.STOP, 10)))
            one(protocol).readStructEnd()
          }

          whenExecuting {
            DefaultValues.decode(protocol) must be(new DefaultValues("delilah"))
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
            one(protocol).readString(); will(returnValue( "Bush"))
            nextRead(e, protocol, new TField("age", TType.I32, 2))
            one(protocol).readI32(); will(returnValue(42))
            endRead(e, protocol)
            /** End of Emperor struct **/

            endRead(e, protocol)
          }

          whenExecuting {
            Empire.decode(protocol) must be(new Empire(
              "United States of America",
              Utilities.makeList("connecticut", "california"),
              new Emperor("Bush", 42)))
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
            one(protocol).writeI32(13)
            endWrite(e, protocol)

            endWrite(e, protocol)
          }

          whenExecuting {
            new Empire(
              "Canada",
              Utilities.makeList("Manitoba", "Alberta"),
              new Emperor("Larry", 13)
            ).write(protocol) must be(())
          }
        }
      }

      "exception" in { _ =>
        (new Xception(1, "boom")).isInstanceOf[Exception] must be(true)
        new Xception(2, "kathunk").getMessage must be("kathunk")
      }

      "exception getMessage" in { _ =>
        new StringMsgException(1, "jeah").getMessage must be("jeah")
        new NonStringMessageException(5).getMessage must be("5")
      }

      "with more than 22 fields" should {
        "apply" in { _ =>
          new Biggie.Builder().build().getNum25() must be(25)
        }

        "two default object must be equal" in { _ =>
          new Biggie.Builder().build() must be(new Biggie.Builder().build())
        }

        "copy and equals" in { _ =>
          new Biggie.Builder().build().copy().num10(-5).build() must be(new Biggie.Builder().num10(-5).build())
        }

        "hashCode is the same for two similar objects" in { _ =>
          new Biggie.Builder().build().hashCode must be(new Biggie.Builder().build().hashCode)
          new Biggie.Builder().num10(-5).build().hashCode must be(new Biggie.Builder().num10(-5).build().hashCode)
        }

        "hashCode is different for two different objects" in { _ =>
          new Biggie.Builder().num10(-5).build().hashCode must not be(new Biggie.Builder().build().hashCode)
        }

        "toString" in { _ =>
          new Biggie.Builder().build().toString must be(("Biggie(" + 1.to(25).map(_.toString).mkString(",") + ")"))
        }
      }
    }

    "unions" should {
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

        "instantiate" in { _ =>
          intercept[NullPointerException] {
            Bird.newRaptor(null)
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
            Bird.decode(protocol) must be(Bird.newHummingbird("Ruby-Throated"))
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
            Bird.newOwletNightjar("foo").write(protocol)
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

      "unknown field" should {
        "read as unknown" in { _ =>
          val prot = new TBinaryProtocol(new TMemoryBuffer(64))
          val unionField = new NewUnionField(
            14653230,
            new SomeInnerUnionStruct(26, "a_a")
          )
          val newUnion = UnionPostEvolution.newNewField(unionField)
          UnionPostEvolution.encode(newUnion, prot)
          val decoded = UnionPreEvolution.decode(prot)

          // work around weird error when trying to reference java enums from scala.
          // java.lang.AssertionError: thrift/java_test/UnionPreEvolution$AnotherName already declared as ch.epfl.lamp.fjbg.JInnerClassesAttribute$Entry@3ac8b10
          decoded.setField.toString must be("UNKNOWN_UNION_VALUE")
        }
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
            Bird.decode(protocol) must be(Bird.newRaptor(new Raptor(false, "peregrine")))
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
            Bird.newRaptor(new Raptor(true, "Tyto alba")).write(protocol)
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
            Bird.decode(protocol) must be(Bird.newFlock(Utilities.makeList("starling", "kestrel", "warbler")))
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
            Bird.newFlock(Utilities.makeList("starling", "kestrel", "warbler")).write(protocol)
          }
        }
      }

      "primitive field type" in { _ =>
        import thrift.java_def._default_._
        val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
        var original: NaughtyUnion = NaughtyUnion.newValue(1)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) must be(original)
        original = NaughtyUnion.newFlag(true)
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) must be(original)
        original = NaughtyUnion.newText("false")
        NaughtyUnion.encode(original, protocol)
        NaughtyUnion.decode(protocol) must be(original)
      }
    }
  }
}
