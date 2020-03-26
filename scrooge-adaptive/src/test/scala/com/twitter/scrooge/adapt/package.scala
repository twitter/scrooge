package com.twitter.scrooge

import com.twitter.scrooge.adapt.thrift._
import com.twitter.scrooge.adapt.thrift.TestStructUnion.{First, Second}
import java.nio.ByteBuffer
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary

package object adapt {
  implicit val binaryArbitrary: Arbitrary[ByteBuffer] = Arbitrary {
    for {
      bytes <- Arbitrary.arbitrary[Array[Byte]]
      offset <- Gen.choose(0, bytes.length)
      len <- Gen.choose(0, bytes.length - offset)
    } yield {
      ByteBuffer.wrap(bytes, offset, len)
    }
  }

  implicit val testStructArbitrary: Arbitrary[TestStruct] = Arbitrary {
    for {
      boolField <- arbitrary[Boolean]
      byteField <- arbitrary[Byte]
      shortField <- arbitrary[Short]
      intField <- arbitrary[Int]
      longField <- arbitrary[Long]
      doubleField <- arbitrary[Double]
      stringField <- arbitrary[String]
      binaryField <- arbitrary[ByteBuffer]
      optionalField <- arbitrary[Option[Boolean]]
      listField <- arbitrary[Seq[Boolean]]
      setField <- arbitrary[Set[Boolean]]
      mapField <- arbitrary[Map[Boolean, Boolean]]
      annotatedId <- arbitrary[Long]
      tpe <- arbitrary[String]
      klass <- arbitrary[Option[String]]
      optionalField2 <- arbitrary[Option[String]]
      optionalFieldWithDefaultValue <- arbitrary[String]
      negativeField <- arbitrary[Boolean]
      snakeCase <- arbitrary[Boolean]
      endOffset <- arbitrary[Boolean]
    } yield TestStruct(
      boolField,
      byteField,
      shortField,
      intField,
      longField,
      doubleField,
      stringField,
      binaryField,
      optionalField,
      listField,
      setField,
      mapField,
      annotatedId,
      tpe,
      klass,
      optionalField2,
      optionalFieldWithDefaultValue,
      negativeField,
      snakeCase,
      endOffset
    )
  }

  implicit val testNestedStructArbitrary: Arbitrary[TestNestedStruct] = Arbitrary {
    for {
      field <- arbitrary[TestStruct]
      tpe <- arbitrary[TestStruct]
      klass <- arbitrary[Option[TestStruct]]
      optionalField <- arbitrary[Option[TestStruct]]
      seqField <- arbitrary[Seq[TestStruct]]
      setField <- arbitrary[Set[TestStruct]]
      mapField <- arbitrary[Map[TestStruct, TestStruct]]
    } yield TestNestedStruct(
      field,
      tpe,
      klass,
      optionalField,
      seqField,
      setField,
      mapField
    )
  }

  implicit val testEmptyStructArbitrary: Arbitrary[TestEmptyStruct] = Arbitrary {
    Gen.const(TestEmptyStruct())
  }

  implicit val testDefaultsStructArbitrary: Arbitrary[TestDefaultsStruct] = Arbitrary {
    for {
      boolField <- arbitrary[Boolean]
      shortField <- arbitrary[Short]
      intField <- arbitrary[Int]
    } yield TestDefaultsStruct(boolField, shortField, intField)
  }

  implicit val testOptionalFieldNoDefaultArbitrary: Arbitrary[TestOptionalFieldNoDefault] =
    Arbitrary {
      for {
        boolField <- arbitrary[Boolean]
        intField <- arbitrary[Option[Int]]
      } yield TestOptionalFieldNoDefault(boolField, intField)
    }

  implicit val testRequiredFieldArbitrary: Arbitrary[TestRequiredField] = Arbitrary {
    for {
      requiredField <- arbitrary[Boolean]
      optionalField <- arbitrary[Option[String]]
    } yield TestRequiredField(requiredField, optionalField)
  }

  implicit val testPassthroughFieldsArbitrary: Arbitrary[TestPassthroughFields] = Arbitrary {
    for {
      field <- arbitrary[String]
    } yield TestPassthroughFields(field)
  }

  implicit val testRequiredDefaultsStructArbitrary: Arbitrary[TestRequiredDefaultsStruct] =
    Arbitrary {
      for {
        stringField <- arbitrary[String]
        listField <- arbitrary[Seq[String]]
      } yield TestRequiredDefaultsStruct(stringField, listField)
    }

  implicit val testStructUnionArbitrary: Arbitrary[TestStructUnion] = Arbitrary {
    for {
      first <- arbitrary[TestStruct]
      second <- arbitrary[TestStruct]
      union <- Gen.oneOf(First(first), Second(second))
    } yield union
  }
}
