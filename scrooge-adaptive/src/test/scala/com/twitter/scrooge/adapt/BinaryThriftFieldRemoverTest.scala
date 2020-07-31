package com.twitter.scrooge.adapt

import com.twitter.scrooge.{BinaryThriftStructSerializer, ThriftStruct, ThriftStructCodec}
import com.twitter.scrooge.adapt.testutil.BinaryThriftFieldRemover
import com.twitter.scrooge.adapt.thrift._
import org.apache.thrift.protocol.TProtocolException
import org.junit.runner.RunWith
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers

@RunWith(classOf[JUnitRunner])
class BinaryThriftFieldRemoverTest extends AnyPropSpec with Checkers {
  def removeField[T <: ThriftStruct](t: T, codec: ThriftStructCodec[T], fieldId: Short): T = {
    val ser = BinaryThriftStructSerializer(codec)
    val bytes = ser.toBytes(t)
    val pruned = BinaryThriftFieldRemover.removeField(bytes, fieldId)
    ser.fromBytes(pruned)
  }

  property("Field remove should remove optional field correctly") {
    check {
      forAll { t: TestStruct =>
        val after = removeField(t, TestStruct, 9)
        after.optionalField == None
      }
    }
  }

  property(
    "Field remove should remove optional field with default value correctly, so it's set to default value"
  ) {
    check {
      forAll { t: TestStruct =>
        val before = t.copy(optionalFieldWithDefaultValue = "test")
        val after = removeField(before, TestStruct, 17)
        after.optionalFieldWithDefaultValue == "default_value"
      }
    }
  }

  property("Field remove should remove required field, leading to TProtocolException on read") {
    check {
      forAll { t: TestRequiredDefaultsStruct =>
        val before = t.copy(stringField = "test")
        Prop.throws(classOf[TProtocolException]) {
          removeField(before, TestRequiredDefaultsStruct, 1)
        }
      }
    }
  }

}
