package com.twitter.scrooge.adapt

import com.twitter.scrooge.{
  BinaryThriftStructSerializer,
  LazyBinaryThriftStructSerializer,
  ThriftStruct,
  ThriftStructCodec,
  ThriftStructSerializer
}
import com.twitter.scrooge.adapt.testutil.{
  BinaryThriftFieldRemover,
  ReloadOnceAdaptBinarySerializer
}
import com.twitter.scrooge.adapt.thrift._
import org.apache.thrift.protocol.{TProtocolException, TType}
import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Prop}
import org.scalatest.PropSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers

object Accessor {
  def apply[T](suppliedName: String)(f: T => Any) = new Accessor[T] {
    def name: String = suppliedName
    def apply(t: T): Any = f(t)
  }
}

trait Accessor[-T] extends ((T) => Any) {
  def name: String
  def apply(t: T): Any
}

@RunWith(classOf[JUnitRunner])
class AdaptiveScroogeTest extends PropSpec with Checkers {

  /**
   * Bytes of thrift struct with no fields set, it's still a valid thrift object.
   * Used for testing of absence of required fields.
   */
  val BlankStructBytes = Array[Byte](TType.STOP)

  val NoAccess: Accessor[Any] = Accessor("No access") { _ =>
    ()
  }

  object TestStructAccessors {
    val RequiredFieldAccess: Accessor[TestStruct] =
      Accessor("Required field") { t =>
        t.boolField
      }

    val RequiredFieldsAccess: Accessor[TestStruct] =
      Accessor("Required fields") { t =>
        t.boolField
        t.stringField
      }

    val OptionalFieldAccess: Accessor[TestStruct] =
      Accessor("Optional field") { _.optionalField }

    val OptionalFieldsAccess: Accessor[TestStruct] =
      Accessor("Optional fields") { t =>
        t.optionalField
        t.optionalField2
      }

    val RequiredReservedWordFieldAccess: Accessor[TestStruct] =
      Accessor("Required field named using reserved word") { _.`type` }

    val OptionalReservedWordFieldAccess: Accessor[TestStruct] =
      Accessor("Optional field named using reserved word") { _.`class` }

    val OptionalWithDefaultFieldAccess: Accessor[TestStruct] =
      Accessor("Optional field with default value") {
        _.optionalFieldWithDefaultValue
      }

    val NegativeFieldAccess: Accessor[TestStruct] =
      Accessor("Negative id field") { _.negativeField }

    val UnderscoreFieldsAccess: Accessor[TestStruct] =
      Accessor("Field access via underscore numbered methods") { t =>
        t._1
        t._2
        t._3
      }

    val All = List(
      RequiredFieldAccess,
      RequiredFieldsAccess,
      OptionalFieldAccess,
      OptionalFieldsAccess,
      RequiredReservedWordFieldAccess,
      OptionalReservedWordFieldAccess,
      OptionalWithDefaultFieldAccess,
      NegativeFieldAccess,
      UnderscoreFieldsAccess
    )
  }

  object TestNestedStructAccessors {
    val RequiredFieldAccess: Accessor[TestNestedStruct] =
      Accessor("Required nested struct") { _.field }

    val OptionalFieldAccess: Accessor[TestNestedStruct] =
      Accessor("Optional nested struct") { _.optionalField }

    val ReservedWordFieldAccess: Accessor[TestNestedStruct] =
      Accessor("Nested required struct named using reserved word") { _.`type` }

    val OptionalReservedWordFieldAccess: Accessor[TestNestedStruct] =
      Accessor("Nested optional struct named using reserved word") { _.`class` }

    val All = List(
      RequiredFieldAccess,
      OptionalFieldAccess,
      ReservedWordFieldAccess,
      OptionalReservedWordFieldAccess
    )
  }

  def useAccessPropTestStruct(use: TestStruct => Any)(
    access: TestStruct => Any
  ) = useAccessProp(TestStruct, use)(access)

  def useAccessPropTestNestedStruct(use: TestNestedStruct => Any)(
    access: TestNestedStruct => Any
  ) = useAccessProp(TestNestedStruct, use)(access)

  def useAccessProp[T <: ThriftStruct](
    thriftCodec: ThriftStructCodec[T],
    use: T => Any
  )(access: T => Any)(implicit arb: Arbitrary[T]): Prop =
    forAll { (orig: T) =>
      val eagerSer = BinaryThriftStructSerializer(thriftCodec)
      // Make sure materialization happens
      val trackedReads = 1
      val useThreshold = 1
      val adaptSer =
        ReloadOnceAdaptBinarySerializer(thriftCodec, AdaptSettings(trackedReads, useThreshold))

      val bytes = eagerSer.toBytes(orig)
      for (_ <- 0 until trackedReads) {
        val a = adaptSer.fromBytes(bytes)
        use(a)
      }
      val after = adaptSer.fromBytes(bytes)
      access(after)
      orig == after
    }

  def encodeDecodeTest[T <: ThriftStruct](
    t: T,
    ser: ThriftStructSerializer[T]
  ): Boolean = t == encodeDecode(t, ser)

  def protocolDecodeTest(
    t: TestStruct,
    accessor: Accessor[TestStruct]
  ): Boolean = {
    val eagerSer = BinaryThriftStructSerializer(TestStruct)
    val lazSer = LazyBinaryThriftStructSerializer(TestStruct)
    val bytes = eagerSer.toBytes(t)

    val adaptSer = ReloadOnceAdaptBinarySerializer(TestStruct)
    val ad = adaptSer.fromBytes(bytes) // trigger adapt
    accessor.apply(ad)
    val adaptGenerated = adaptSer.fromBytes(bytes)

    val lazCheck = encodeDecodeTest(adaptGenerated, lazSer)
    val eagerCheck = encodeDecodeTest(adaptGenerated, eagerSer)
    val adaptCheck = encodeDecodeTest(adaptGenerated, adaptSer)
    lazCheck && eagerCheck && adaptCheck
  }

  def encodeDecodeTestAllFormatNestedStruct(tn: TestNestedStruct): Boolean =
    encodeDecodeTest(tn, BinaryThriftStructSerializer(TestNestedStruct)) &&
      encodeDecodeTest(tn, LazyBinaryThriftStructSerializer(TestNestedStruct)) &&
      encodeDecodeTest(tn, ReloadOnceAdaptBinarySerializer(TestNestedStruct))

  def getMaterializedFieldNames(t: ThriftStruct): Seq[String] =
    t.getClass.getDeclaredFields.flatMap { f =>
      val name = f.getName
      if (name.startsWith(AdaptAsmPruner.MemberPrefix)) {
        Some(name.substring(AdaptAsmPruner.MemberPrefix.size))
      } else {
        None
      }
    }

  def getFieldsWithMethodPrefix[T <: ThriftStruct](
    t: T,
    codec: ThriftStructCodec[T],
    prefix: String
  ): Seq[String] = {
    val fieldNames = codec.metaData.fields.map(_.name).toSet
    t.getClass.getDeclaredMethods.flatMap { method =>
      val name = method.getName
      if (name.startsWith(prefix)) {
        val fieldName = name.substring(prefix.size)
        if (fieldNames.contains(fieldName)) Some(fieldName)
        else None
      } else {
        None
      }
    }
  }

  def getFieldsWithSetters[T <: ThriftStruct](
    t: T,
    codec: ThriftStructCodec[T]
  ): Seq[String] =
    getFieldsWithMethodPrefix(t, codec, AdaptAsmPruner.SetterPrefix)

  def getFieldsWithDelegates[T <: ThriftStruct](
    t: T,
    codec: ThriftStructCodec[T]
  ): Seq[String] =
    getFieldsWithMethodPrefix(t, codec, AdaptAsmPruner.DelegatePrefix)

  def encodeDecode[T <: ThriftStruct](
    t: T,
    ser: ThriftStructSerializer[T]
  ) = ser.fromBytes(ser.toBytes(t))

  def toEager[T <: ThriftStruct](t: T, codec: ThriftStructCodec[T]): T =
    encodeDecode(t, BinaryThriftStructSerializer(codec))

  def toLazy[T <: ThriftStruct](t: T, codec: ThriftStructCodec[T]): T =
    encodeDecode(t, LazyBinaryThriftStructSerializer(codec))

  def toAdapt[T <: ThriftStruct](t: T, codec: ThriftStructCodec[T]): T =
    encodeDecode(t, ReloadOnceAdaptBinarySerializer(codec))

  def toBytes(t: TestStruct): Array[Byte] = {
    val ser = BinaryThriftStructSerializer(TestStruct)
    ser.toBytes(t)
  }

  trait Fixture[T <: ThriftStruct] {
    def eagerSer: ThriftStructSerializer[T]
    def adaptSer: ThriftStructSerializer[T]
    def bytes: Array[Byte]
    def recorder: T
    def adapted: T
  }

  def fixture[T <: ThriftStruct](
    t: T,
    codec: ThriftStructCodec[T],
    train: T => Any = NoAccess
  ) = new Fixture[T] {
    val eagerSer = BinaryThriftStructSerializer(codec)
    val bytes = eagerSer.toBytes(t)
    val (adaptSer, recorder, adapted) = {
      val ser = ReloadOnceAdaptBinarySerializer(codec)
      val recorder = ser.fromBytes(bytes)
      train(recorder)
      val adapted = ser.fromBytes(bytes)
      (ser, recorder, adapted)
    }
  }

  def testStructFixture(t: TestStruct, train: TestStruct => Any = NoAccess) =
    fixture(t, TestStruct, train)

  val RemainUnchangedSerDe = "Should remain unchanged through serialization and " +
    "deserialization"

  for (accessor <- TestStructAccessors.All) {
    val name = accessor.name
    property(s"$RemainUnchangedSerDe when $name used but not accessed") {
      check { useAccessPropTestStruct(accessor.apply _)(NoAccess.apply _) }
    }

    property(s"$RemainUnchangedSerDe when $name not used but accessed") {
      check { useAccessPropTestStruct(NoAccess.apply _)(accessor.apply _) }
    }

    property(s"$RemainUnchangedSerDe when $name used and accessed") {
      check { useAccessPropTestStruct(accessor.apply _)(accessor.apply _) }
    }
  }

  for (accessor <- TestNestedStructAccessors.All) {
    val name = accessor.name
    property(s"$RemainUnchangedSerDe when $name used but not accessed") {
      check { useAccessPropTestNestedStruct(accessor.apply _)(NoAccess.apply _) }
    }

    property(s"$RemainUnchangedSerDe when $name not used but accessed") {
      check { useAccessPropTestNestedStruct(NoAccess.apply _)(accessor.apply _) }
    }

    property(s"$RemainUnchangedSerDe when $name used and accessed") {
      check { useAccessPropTestNestedStruct(accessor.apply _)(accessor.apply _) }
    }
  }

  property(
    "Parsing required field not set should throw exception even when " +
      "required field is unused"
  ) {
    check {
      forAll { t: TestRequiredField =>
        val f = fixture(t, TestRequiredField)
        Prop.throws(classOf[TProtocolException]) {
          f.adaptSer.fromBytes(BlankStructBytes)
        }
      }
    }
  }

  property(
    "Parsing required field not set should throw exception even when " +
      "required field is used"
  ) {
    check {
      forAll { t: TestRequiredField =>
        val f = fixture[TestRequiredField](t, TestRequiredField, _.requiredField)
        Prop.throws(classOf[TProtocolException]) {
          f.adaptSer.fromBytes(BlankStructBytes)
        }
      }
    }
  }

  property(
    s"$RemainUnchangedSerDe when trained with optional field used but " +
      s"not set in incoming event"
  ) {
    check {
      forAll { (t: TestStruct) =>
        val f = testStructFixture(t, _.optionalField)
        encodeDecodeTest(t.unsetOptionalField, f.adaptSer)
      }
    }
  }

  property(
    "Adapt generated object should be able to write using other " +
      "protocols when no field in adapt materialized"
  ) {
    check {
      forAll { t: TestStruct =>
        protocolDecodeTest(t, NoAccess)
      }
    }
  }

  property(
    "Adapt generated object should be able to write using other " +
      "protocols when some fields in adapt materialized"
  ) {
    check {
      forAll { t: TestStruct =>
        protocolDecodeTest(t, TestStructAccessors.RequiredFieldsAccess)
      }
    }
  }

  property("Should work well with struct field named passthroughFields") {
    check {
      forAll { t: TestPassthroughFields =>
        val f = fixture(t, TestPassthroughFields)
        encodeDecodeTest(t, f.adaptSer)
      }
    }
  }

  property(
    "Calling write on the struct should not result in fields being " +
      "considered accessed"
  ) {
    check {
      forAll { t: TestRequiredField =>
        val f = fixture[TestRequiredField](t, TestRequiredField)
        // This calls write on recorder
        f.eagerSer.toBytes(f.recorder)
        getMaterializedFieldNames(f.adapted).isEmpty
      }
    }
  }

  val SettersGone = "setters should be gone for unused fields"
  for (accessor <- TestStructAccessors.All) {
    property(s"$SettersGone when ${accessor.name} used") {
      check {
        forAll { t: TestStruct =>
          val f = testStructFixture(t, accessor)
          val setters = getFieldsWithSetters(f.adapted, TestStruct).toSet
          val usedFields = getMaterializedFieldNames(f.adapted).toSet
          setters == usedFields
        }
      }
    }
  }

  val DelegatesGone = "delegates should be gone"
  for (accessor <- TestStructAccessors.All) {
    property(s"$DelegatesGone when ${accessor.name} used") {
      check {
        forAll { t: TestStruct =>
          val f = testStructFixture(t, accessor)
          val delegates = getFieldsWithDelegates(f.adapted, TestStruct).toSet
          delegates.isEmpty
        }
      }
    }
  }

  /*
  Pants doesn't have a way of specifying flag for generating Adaptive Scrooge code,
  so this test fails. Commenting for now.
  TODO(pankajg) Uncomment this after required pants support is available.
  property("Fields accessed less times than materialization threshold should " +
    "not be materialized") {
    check {
      forAll { t: TestRequiredField =>
        val eagerSer = BinaryThriftStructSerializer(TestRequiredField)
        val trackedReads = 3
        val useThreshold = 2
        val adaptSer = ReloadOnceAdaptBinarySerializer(
          TestRequiredField,
          AdaptSettings(trackedReads, useThreshold))

        val bytes = eagerSer.toBytes(t)

        for (_ <- 0 until 2) {
          val recorder = adaptSer.fromBytes(bytes)
          recorder.requiredField
        }

        // Access this field less than threshold times
        val recorder = adaptSer.fromBytes(bytes)
        recorder.optionalField

        val ad = adaptSer.fromBytes(bytes)
        val materialized = getMaterializedFieldNames(ad)
        materialized == Seq("requiredField")
      }
    }
  }
   */

  property("Eager scrooge nested in Adaptive scrooge should encode decode fine") {
    check {
      forAll { (tn: TestNestedStruct, t: TestStruct) =>
        val tnAdapt = toAdapt(tn, TestNestedStruct)
        val tEager = toEager(t, TestStruct)
        tnAdapt.copy(field = tEager)
        encodeDecodeTestAllFormatNestedStruct(tnAdapt)
      }
    }
  }

  property("Lazy scrooge nested in Adaptive scrooge should encode decode fine") {
    check {
      forAll { (tn: TestNestedStruct, t: TestStruct) =>
        val tnAdapt = toAdapt(tn, TestNestedStruct)
        val tLazy = toLazy(t, TestStruct)
        tnAdapt.copy(field = tLazy)
        encodeDecodeTestAllFormatNestedStruct(tnAdapt)
      }
    }
  }

  property("Adapt scrooge nested in Eager scrooge should encode decode fine") {
    check {
      forAll { (tn: TestNestedStruct, t: TestStruct) =>
        val tnEager = toEager(tn, TestNestedStruct)
        val tAdapt = toAdapt(t, TestStruct)
        tnEager.copy(field = tAdapt)
        encodeDecodeTestAllFormatNestedStruct(tnEager)
      }
    }
  }

  property("Adapt scrooge nested in Lazy scrooge should encode decode fine") {
    check {
      forAll { (tn: TestNestedStruct, t: TestStruct) =>
        val tnLazy = toLazy(tn, TestNestedStruct)
        val tAdapt = toAdapt(t, TestStruct)
        tnLazy.copy(field = tAdapt)
        encodeDecodeTestAllFormatNestedStruct(tnLazy)
      }
    }
  }

  property("Adapt serializer should be concurrency safe") {
    check {
      forAll { t: TestStruct =>
        testStructFixture(t) // Trigger adaptation
        val results = Par.calcInParallel(100) { _ =>
          // Build serializer per thread, serializer is not thread safe
          // and is not what we're testing.
          val adaptSer = ReloadOnceAdaptBinarySerializer(TestStruct)
          encodeDecodeTest(t, adaptSer)
        }
        results.forall(_ == true)
      }
    }
  }

  property(
    "Adapt serializer should pick up the default value when present " +
      "for optional values, when optional field is used"
  ) {
    check {
      forAll { t: TestStruct =>
        val before = t.copy(optionalFieldWithDefaultValue = "test")
        val eagerSer = BinaryThriftStructSerializer(TestStruct)
        val bytes = eagerSer.toBytes(before)
        val pruned = BinaryThriftFieldRemover.removeField(bytes, 17)
        val adaptSer = ReloadOnceAdaptBinarySerializer(TestStruct)
        val ad = adaptSer.fromBytes(pruned) // trigger adaptation
        // Make sure optional field with default value is used
        ad.optionalFieldWithDefaultValue
        val after = adaptSer.fromBytes(pruned)
        after.optionalFieldWithDefaultValue == "default_value"
      }
    }
  }

  property(
    "Adapt serializer should pick up the default value when present " +
      "for optional values, when optional field is not used"
  ) {
    check {
      forAll { t: TestStruct =>
        val before = t.copy(optionalFieldWithDefaultValue = "test")
        val eagerSer = BinaryThriftStructSerializer(TestStruct)
        val bytes = eagerSer.toBytes(before)
        val pruned = BinaryThriftFieldRemover.removeField(bytes, 17)
        val adaptSer = ReloadOnceAdaptBinarySerializer(TestStruct)
        adaptSer.fromBytes(pruned) // trigger adaptation
        val after = adaptSer.fromBytes(pruned)
        after.optionalFieldWithDefaultValue == "default_value"
      }
    }
  }

  property(
    "Adapt serializer should throw TProtocolException when required " +
      "field not present, even if default value is specified for it in the " +
      "thrift definition"
  ) {
    check {
      forAll { t: TestRequiredDefaultsStruct =>
        val before = t.copy(stringField = "test")
        val eagerSer = BinaryThriftStructSerializer(TestRequiredDefaultsStruct)
        val bytes = eagerSer.toBytes(before)
        val pruned = BinaryThriftFieldRemover.removeField(bytes, 1)
        val adaptSer = ReloadOnceAdaptBinarySerializer(TestRequiredDefaultsStruct)
        adaptSer.fromBytes(bytes) // trigger adaptation
        Prop.throws(classOf[TProtocolException]) {
          adaptSer.fromBytes(pruned)
        }
      }
    }
  }

  property("Adapt struct inside union should work correctly") {
    check {
      forAll { t: TestStructUnion =>
        val f = fixture(t, TestStructUnion)
        encodeDecodeTest(f.adapted, BinaryThriftStructSerializer(TestStructUnion))
      }
    }
  }
}
