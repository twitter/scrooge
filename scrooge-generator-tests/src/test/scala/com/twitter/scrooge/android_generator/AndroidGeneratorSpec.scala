package com.twitter.scrooge.android_generator

import android_thrift_default_namespace.{
  ConstructorRequiredStruct,
  DeepValidationStruct,
  DeepValidationUnion
}
import com.github.mustachejava.Mustache
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{ResolvedDocument, TypeResolver, _}
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.testutil.Utils.getFileContents
import java.util
import java.util.{List => JList, Map => JMap, Set => JSet}
import org.apache.thrift.protocol.TProtocolException
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import thrift.complete.android.test1.{SimpleWithDefaults, StructXA, StructXB}
import thrift.complete.android.test2.ComplexCollections

class AndroidGeneratorSpec extends Spec {
  def generateDoc(str: String) = {
    val importer = Importer(
      Seq(
        "src/test/resources/test_thrift",
        "scrooge-generator-tests/src/test/resources/test_thrift"
      )
    )
    val parser = new ThriftParser(importer, true)
    val doc = parser.parse(str, parser.document)
    TypeResolver()(doc).document
  }

  val templateCache = new TrieMap[String, Mustache]

  def getGenerator(doc: Document) = {
    new AndroidGenerator(
      ResolvedDocument(doc, new TypeResolver),
      "thrift",
      templateCache
    )
  }

  def populateTestData = {
    val list = new util.ArrayList[Integer]
    list.add(10)
    list.add(20)
    list.add(30)

    val set = new util.HashSet[Integer]
    set.add(11)
    set.add(22)
    set.add(33)
    set.add(44)
    set.add(55)

    val map = new util.HashMap[Integer, Integer]
    map.put(100, 100)
    map.put(200, 200)
    map.put(300, 300)
    map.put(400, 400)

    (new SimpleWithDefaults.Builder)
      .set(SimpleWithDefaults.Z, false)
      .set(SimpleWithDefaults.B, 255.toByte)
      .set(SimpleWithDefaults.S, 12345.toShort)
      .set(SimpleWithDefaults.I, 105020401)
      .set(SimpleWithDefaults.J, 1234000000L)
      .set(SimpleWithDefaults.D, 9876.54)
      .set(SimpleWithDefaults.STR, "Changed Value")
      .set(SimpleWithDefaults.I_LIST, list)
      .set(SimpleWithDefaults.I_SET, set)
      .set(SimpleWithDefaults.I_MAP, map)
      .build()

  }

  "Generator" should {
    System.setProperty("mustache.debug", "true")
    "populate default values in structs" in {
      val defaultValues = (new SimpleWithDefaults.Builder).build()

      (defaultValues.get(SimpleWithDefaults.Z): Boolean) must be(true)
      (defaultValues.get(SimpleWithDefaults.B): Byte) must be(1)
      (defaultValues.get(SimpleWithDefaults.S): Short) must be(1)
      (defaultValues.get(SimpleWithDefaults.I): Integer) must be(1)
      (defaultValues.get(SimpleWithDefaults.J): Long) must be(1)
      (defaultValues.get(SimpleWithDefaults.D): Double) must be(1.0)
      (defaultValues.get(SimpleWithDefaults.STR): String) must be("yo")
      val l: util.ArrayList[Integer] = defaultValues.get(SimpleWithDefaults.I_LIST)
      l.size must be(1)
      l.contains(1) must be(true)

      val i_set: util.HashSet[Integer] =
        defaultValues.get(SimpleWithDefaults.I_SET): util.HashSet[Integer]
      i_set.size must be(1)
      i_set.contains(1) must be(true)

      val i_map: util.HashMap[Integer, Integer] = defaultValues.get(SimpleWithDefaults.I_MAP)
      i_map.size must be(1)
      i_map.get(1) must be(1)
    }
    "handle updated values in structs" in {
      val simple = populateTestData
      (simple.get(SimpleWithDefaults.Z): Boolean) must be(false)
      (simple.get(SimpleWithDefaults.B): Byte) must be(255.toByte)
      (simple.get(SimpleWithDefaults.S): Short) must be(12345.toShort)
      (simple.get(SimpleWithDefaults.I): Integer) must be(105020401)
      (simple.get(SimpleWithDefaults.J): Long) must be(1234000000L)
      (simple.get(SimpleWithDefaults.D): Double) must be(9876.54)
      (simple.get(SimpleWithDefaults.STR): String) must be("Changed Value")
      val l: util.ArrayList[Integer] = simple.get(SimpleWithDefaults.I_LIST)
      l.size must be(3)
      l.contains(1) must be(false)
      l.contains(10) must be(true)

      val i_set: util.HashSet[Integer] = simple.get(SimpleWithDefaults.I_SET): util.HashSet[Integer]
      i_set.size must be(5)
      i_set.contains(1) must be(false)
      i_set.contains(55) must be(true)

      val i_map: util.HashMap[Integer, Integer] = simple.get(SimpleWithDefaults.I_MAP)
      i_map.size must be(4)
      i_map.get(1) must be(null)
      i_map.get(300) must be(300)
    }
    "return same hashcode for same values" in {
      val data1 = populateTestData
      val data2 = populateTestData
      data1.hashCode must be(data2.hashCode)
    }
    "return valid compareTo result" in {
      val dataSmall1 = (new SimpleWithDefaults.Builder)
        .set(SimpleWithDefaults.I, 100)
        .build()
      val dataSmall2 = (new SimpleWithDefaults.Builder)
        .set(SimpleWithDefaults.I, 100)
        .build()
      val dataLarge = (new SimpleWithDefaults.Builder)
        .set(SimpleWithDefaults.I, 200)
        .build()

      dataSmall1.compareTo(dataSmall2) must be(0)
      dataSmall1.compareTo(dataLarge) < 0 must be(true)
    }
    "handle struct rhs assignment" in {
      val const = thrift.complete.android.test1.Constants.ListOfComplexStructs
      const.size() must be(3)
      val second = const.get(1)

      val structXA = second.get(StructXB.STRUCT_FIELD): StructXA
      (second.get(StructXB.SNAKE_CASE_FIELD): Long) must be(71)
      (second.get(StructXB.CAMEL_CASE_FIELD): Long) must be(72)
      (second.get(StructXB.REQUIRED_FIELD): Long) must be(73)
      (structXA.get(StructXA.ID): Long) must be(71)
    }
    "populate complex collections" in {
      val xa1 = (new StructXA.Builder).set(StructXA.ID, 100L).build
      val xa2 = (new StructXA.Builder).set(StructXA.ID, 321L).build
      val xa3 = (new StructXA.Builder).set(StructXA.ID, 333L).build
      val xa4 = (new StructXA.Builder).set(StructXA.ID, 555L).build
      val xa5 = (new StructXA.Builder).set(StructXA.ID, 666L).build

      val xb1 = (new StructXB.Builder)
        .set(StructXB.SNAKE_CASE_FIELD, 10L)
        .set(StructXB.CAMEL_CASE_FIELD, 20L)
        .set(StructXB.OPTIONAL_FIELD, "very optional field")
        .set(StructXB.REQUIRED_FIELD, 700L)
        .set(StructXB.STRUCT_FIELD, xa1)
        .set(StructXB.DEFAULT_FIELD, 300L)
        .build

      val xb2 = (new StructXB.Builder)
        .set(StructXB.SNAKE_CASE_FIELD, 12L)
        .set(StructXB.CAMEL_CASE_FIELD, 22L)
        .set(StructXB.OPTIONAL_FIELD, "very very optional field")
        .set(StructXB.REQUIRED_FIELD, 702L)
        .set(StructXB.STRUCT_FIELD, xa2)
        .set(StructXB.DEFAULT_FIELD, 302L)
        .build

      val xb3 = (new StructXB.Builder)
        .set(StructXB.SNAKE_CASE_FIELD, 13L)
        .set(StructXB.CAMEL_CASE_FIELD, 23L)
        .set(StructXB.OPTIONAL_FIELD, "very very very optional field")
        .set(StructXB.REQUIRED_FIELD, 703L)
        .set(StructXB.STRUCT_FIELD, xa3)
        .set(StructXB.DEFAULT_FIELD, 303L)
        .build

      val xb4 = (new StructXB.Builder)
        .set(StructXB.SNAKE_CASE_FIELD, 413L)
        .set(StructXB.CAMEL_CASE_FIELD, 423L)
        .set(StructXB.OPTIONAL_FIELD, "4: optional field")
        .set(StructXB.REQUIRED_FIELD, 4703L)
        .set(StructXB.STRUCT_FIELD, xa3)
        .set(StructXB.DEFAULT_FIELD, 4303L)
        .build

      val xb5 = (new StructXB.Builder)
        .set(StructXB.SNAKE_CASE_FIELD, 513L)
        .set(StructXB.CAMEL_CASE_FIELD, 523L)
        .set(StructXB.OPTIONAL_FIELD, "5 very very very optional field")
        .set(StructXB.REQUIRED_FIELD, 5703L)
        .set(StructXB.STRUCT_FIELD, xa4)
        .set(StructXB.DEFAULT_FIELD, 5303L)
        .build

      val xb6 = (new StructXB.Builder)
        .set(StructXB.SNAKE_CASE_FIELD, 613L)
        .set(StructXB.CAMEL_CASE_FIELD, 623L)
        .set(StructXB.OPTIONAL_FIELD, "6 very very very optional field")
        .set(StructXB.REQUIRED_FIELD, 6703L)
        .set(StructXB.STRUCT_FIELD, xa5)
        .set(StructXB.DEFAULT_FIELD, 6303L)
        .build

      val xb_key_1 = xb1.deepCopy
      val xb_key_2 = xb2.deepCopy
      val xb_key_3 = xb3.deepCopy

      val list_in_complex_map1 = new util.ArrayList[String]
      list_in_complex_map1.add("first")
      list_in_complex_map1.add("second")
      list_in_complex_map1.add("third")

      val list_in_complex_map2 = new util.ArrayList[String]
      list_in_complex_map2.add("one")
      list_in_complex_map2.add("two")
      list_in_complex_map2.add("three")

      val map_in_complex_list1 = new util.HashMap[String, Integer]
      map_in_complex_list1.put("data_1", 1111)
      map_in_complex_list1.put("data_2", 1222)
      map_in_complex_list1.put("data_3", 1333)
      map_in_complex_list1.put("data_4", 1444)
      map_in_complex_list1.put("data_5", 1555)

      val map_in_complex_list2 = new util.HashMap[String, Integer]
      map_in_complex_list2.put("data_1", 2111)
      map_in_complex_list2.put("data_2", 2222)
      map_in_complex_list2.put("data_3", 2333)
      map_in_complex_list2.put("data_4", 2444)
      map_in_complex_list2.put("data_5", 2555)

      val map_in_complex_list3 = new util.HashMap[String, Integer]
      map_in_complex_list3.put("data_1", 2111)
      map_in_complex_list3.put("data_2", 2222)
      map_in_complex_list3.put("data_3", 2333)
      map_in_complex_list3.put("data_4", 2444)
      map_in_complex_list3.put("data_5", 2555)

      val list_in_super_complex1 = new util.ArrayList[String]
      list_in_super_complex1.add("one")
      list_in_super_complex1.add("two")
      list_in_super_complex1.add("three")

      val list_in_super_complex2 = new util.ArrayList[String]
      list_in_super_complex2.add("second one")
      list_in_super_complex2.add("second two")
      list_in_super_complex2.add("second three")

      val list_in_super_complex3 = new util.ArrayList[String]
      list_in_super_complex3.add("three one")
      list_in_super_complex3.add("three two")
      list_in_super_complex3.add("three three")

      val map_in_super_complex1 = new util.HashMap[String, util.ArrayList[String]]
      map_in_super_complex1.put("key_one_one_empty", new util.ArrayList[String])
      map_in_super_complex1.put("key_one_two", list_in_super_complex1)
      map_in_super_complex1.put("key_one_three", list_in_super_complex2)

      val map_in_super_complex2 = new util.HashMap[String, util.ArrayList[String]]
      map_in_super_complex2.put("key_two_one_empty", new util.ArrayList[String])
      map_in_super_complex2.put("key_two_two", list_in_super_complex3)

      val map_in_super_complex3 = new util.HashMap[String, util.ArrayList[String]]
      map_in_super_complex3.put("key_three_one_empty", list_in_super_complex3)

      val set_in_super_complex1 = new util.HashSet[util.HashMap[String, util.ArrayList[String]]]
      set_in_super_complex1.add(map_in_super_complex1)
      set_in_super_complex1.add(map_in_super_complex2)

      val set_in_super_complex2 = new util.HashSet[util.HashMap[String, util.ArrayList[String]]]
      set_in_super_complex2.add(new util.HashMap[String, util.ArrayList[String]])

      val set_in_super_complex3 = new util.HashSet[util.HashMap[String, util.ArrayList[String]]]
      set_in_super_complex3.add(map_in_super_complex2)

      val complexColl = (new ComplexCollections.Builder)
        .addTo(ComplexCollections.LXB, xb1.deepCopy)
        .addTo(ComplexCollections.LXB, xb2.deepCopy)
        .addTo(ComplexCollections.LXB, xb2.deepCopy)
        .addTo(ComplexCollections.LXB, xb2.deepCopy)
        .addTo(ComplexCollections.LXB, xb3.deepCopy)
        .addTo(ComplexCollections.SXB, xb4)
        .addTo(ComplexCollections.SXB, xb5)
        .addTo(ComplexCollections.SXB, xb1.deepCopy)
        .addTo(ComplexCollections.SXB, xb2.deepCopy)
        .addTo(ComplexCollections.SXB, xb3.deepCopy)
        .putTo(ComplexCollections.MXB, xb_key_1, xb3.deepCopy)
        .putTo(ComplexCollections.MXB, xb_key_2, xb2.deepCopy)
        .putTo(ComplexCollections.MXB, xb_key_3, xb1.deepCopy)
        .putTo(ComplexCollections.COMPLEX_MAP, "first_element", list_in_complex_map1)
        .putTo(ComplexCollections.COMPLEX_MAP, "second_element", list_in_complex_map2)
        .addTo(ComplexCollections.COMPLEX_LIST, map_in_complex_list1)
        .addTo(ComplexCollections.COMPLEX_LIST, map_in_complex_list2)
        .addTo(ComplexCollections.COMPLEX_LIST, map_in_complex_list3)
        .addTo(ComplexCollections.SUPER_COMPLEX_COLLECTION, set_in_super_complex1)
        .addTo(
          ComplexCollections.SUPER_COMPLEX_COLLECTION,
          new util.HashSet[util.HashMap[String, util.ArrayList[String]]]
        )
        .addTo(ComplexCollections.SUPER_COMPLEX_COLLECTION, set_in_super_complex2)
        .build

      val structList: util.ArrayList[StructXB] = complexColl.get(ComplexCollections.LXB)
      structList.size must be(5)
      val xb1_candidate = structList.get(0)
      xb1_candidate.hashCode must be(xb1.hashCode)
      (xb1_candidate.get(StructXB.SNAKE_CASE_FIELD): Long) must be(10L)
      (xb1_candidate.get(StructXB.CAMEL_CASE_FIELD): Long) must be(20L)
      (xb1_candidate.get(StructXB.OPTIONAL_FIELD): String) must be("very optional field")
      (xb1_candidate.get(StructXB.REQUIRED_FIELD): Long) must be(700L)
      val xb1_xa1_candidate: StructXA = xb1_candidate.get(StructXB.STRUCT_FIELD)
      (xb1_xa1_candidate.get(StructXA.ID): Long) must be(100L)

      val structSet: util.HashSet[StructXB] = complexColl.get(ComplexCollections.SXB)
      structSet.size must be(5)
      structSet.contains(xb4) must be(true)
      structSet.contains(xb5) must be(true)
      structSet.contains(xb6) must be(false)

      val structMap: util.HashMap[String, StructXB] = complexColl.get(ComplexCollections.MXB)
      structMap.size must be(3)
      structMap.containsKey(xb_key_1) must be(true)
      structMap.containsKey(xb_key_2) must be(true)
      structMap.containsKey(xb_key_3) must be(true)
      structMap.containsKey(xb4) must be(false)
      val xb2_copied = structMap.get(xb_key_2)
      xb2_copied.hashCode must be(xb2.hashCode)
      (xb2_copied.get(StructXB.SNAKE_CASE_FIELD): Long) must be(12L)
      (xb2_copied.get(StructXB.CAMEL_CASE_FIELD): Long) must be(22L)
      (xb2_copied.get(StructXB.OPTIONAL_FIELD): String) must be("very very optional field")
      (xb2_copied.get(StructXB.REQUIRED_FIELD): Long) must be(702L)
      val xb2_xa1_candidate: StructXA = xb2_copied.get(StructXB.STRUCT_FIELD)
      (xb2_xa1_candidate.get(StructXA.ID): Long) must be(321L)
      (xb2_copied.get(StructXB.DEFAULT_FIELD): Long) must be(302L)

      val complex_map_value: util.HashMap[String, util.ArrayList[String]] =
        complexColl.get(ComplexCollections.COMPLEX_MAP)
      complex_map_value.size must be(2)
      complex_map_value.get("first_element") must be(list_in_complex_map1)
      complex_map_value.get("second_element").hashCode must be(list_in_complex_map2.hashCode)
      complex_map_value.get("second_element").hashCode must not be (list_in_complex_map1.hashCode)

      val complex_list_value: util.ArrayList[util.HashMap[String, Integer]] =
        complexColl.get(ComplexCollections.COMPLEX_LIST)
      complex_list_value.size must be(3)
      (complex_list_value.get(2): util.HashMap[String, Integer]) must be(map_in_complex_list3)

      val super_complex_list: util.ArrayList[
        util.HashSet[util.HashMap[String, util.ArrayList[String]]]] =
        complexColl.get(ComplexCollections.SUPER_COMPLEX_COLLECTION)
      super_complex_list.size must be(3)
      super_complex_list.contains(set_in_super_complex1) must be(true)
      super_complex_list.contains(set_in_super_complex2) must be(true)
      super_complex_list.contains(set_in_super_complex3) must be(false)
      super_complex_list.get(0) must be(set_in_super_complex1)
      super_complex_list.get(0).equals(set_in_super_complex1) must be(true)

      val super_complex_list_set: util.HashSet[util.HashMap[String, util.ArrayList[String]]] =
        super_complex_list.get(0)
      super_complex_list_set.contains(map_in_super_complex1) must be(true)
      super_complex_list_set.contains(map_in_super_complex1) must be(true)
      super_complex_list_set.contains(map_in_super_complex3) must be(false)

    }

    "populate enum controller" in {
      val doc = generateDoc(getFileContents("test_thrift/enum.thrift"))
      val controller = new EnumController(doc.enums(0), getGenerator(doc), doc.namespace("android"))
      controller.name must be("test")
      controller.constants(0).last must be(false)
      controller.constants(1).last must be(true)
      controller.constants(0).name must be("foo")
      controller.namespace must be("com.twitter.thrift")
    }

    "constructor required fields" should {
      "not be optional in the contructor with args" in {
        val struct = new ConstructorRequiredStruct(
          1L,
          "test",
          3L,
          4L,
          5L
        )
        struct.getFieldValue(ConstructorRequiredStruct.CONSTRUCTION_REQUIRED_FIELD) must be(3)
        struct.isSet(ConstructorRequiredStruct.CONSTRUCTION_REQUIRED_FIELD) must be(true)
      }
    }

    "validate" should {
      "throw an exception when missing a field" in {
        val struct = new ConstructorRequiredStruct(
          1L,
          null,
          3L,
          4L,
          5L
        )
        val exception = intercept[TProtocolException](struct.validate())
        val expected = "Required field 'requiredField' was not present! Struct: " + struct.toString
        exception.getMessage must be(expected)
      }
      "not throw when required fields are present" in {
        val struct = new ConstructorRequiredStruct(
          1L,
          "present",
          3L,
          4L,
          5L
        )
        struct.validate()
      }
    }

    "validateNewInstance" should {
      val validInstance = new ConstructorRequiredStruct()
      validInstance.setFieldValue(ConstructorRequiredStruct.OPTIONAL_FIELD, 1L)
      validInstance.setFieldValue(ConstructorRequiredStruct.REQUIRED_FIELD, "test")
      validInstance.setFieldValue(ConstructorRequiredStruct.CONSTRUCTION_REQUIRED_FIELD, 3L)
      validInstance.setFieldValue(ConstructorRequiredStruct.DEFAULT_REQUIREDNESS_FIELD, 4L)

      val missingRequiredFieldInstance = new ConstructorRequiredStruct()
      missingRequiredFieldInstance.setFieldValue(ConstructorRequiredStruct.OPTIONAL_FIELD, 1L)
      missingRequiredFieldInstance
        .setFieldValue(ConstructorRequiredStruct.CONSTRUCTION_REQUIRED_FIELD, 3L)
      missingRequiredFieldInstance
        .setFieldValue(ConstructorRequiredStruct.DEFAULT_REQUIREDNESS_FIELD, 4L)

      val missingConstructionRequiredFieldInstance = new ConstructorRequiredStruct()
      missingConstructionRequiredFieldInstance
        .setFieldValue(ConstructorRequiredStruct.OPTIONAL_FIELD, 1L)
      missingConstructionRequiredFieldInstance
        .setFieldValue(ConstructorRequiredStruct.REQUIRED_FIELD, "test")
      missingConstructionRequiredFieldInstance
        .setFieldValue(ConstructorRequiredStruct.DEFAULT_REQUIREDNESS_FIELD, 4L)

      val constructionRequiredErrorMessage =
        "Construction required field 'constructionRequiredField' in type 'ConstructorRequiredStruct' was not present."
      val requiredErrorMessage =
        "Required field 'requiredField' in type 'ConstructorRequiredStruct' was not present."

      def validateMissingConstructionRequiredField(
        struct: DeepValidationStruct,
        number: Int = 1
      ): Unit = {
        val result = DeepValidationStruct.validateNewInstance(struct).asScala
        result must have size number
        result.foreach { errorMessage =>
          errorMessage must be(constructionRequiredErrorMessage)
        }
      }

      def buildDeepValidationStruct(
        listField: JList[ConstructorRequiredStruct] = Seq(validInstance).asJava,
        setField: JSet[ConstructorRequiredStruct] = Set(validInstance).asJava,
        requiredField: ConstructorRequiredStruct = validInstance,
        inMapKey: JMap[ConstructorRequiredStruct, String] = Map(validInstance -> "value").asJava,
        inMapValue: JMap[String, ConstructorRequiredStruct] = Map("value" -> validInstance).asJava,
        crazyEmbedding: JMap[
          JSet[JList[ConstructorRequiredStruct]],
          JSet[
            JList[ConstructorRequiredStruct]
          ]] = Map(Set(Seq(validInstance).asJava).asJava ->
            Set(Seq(validInstance).asJava).asJava).asJava,
        optionalField: ConstructorRequiredStruct = validInstance
      ): DeepValidationStruct = {
        new DeepValidationStruct(
          listField,
          setField,
          optionalField,
          requiredField,
          inMapKey,
          inMapValue,
          crazyEmbedding,
          null
        )
      }

      "return an empty list when required fields are present" in {
        val result = ConstructorRequiredStruct.validateNewInstance(validInstance).asScala
        result must be(List.empty)
      }
      "return a MissingRequiredField when missing a required field" in {
        val result =
          ConstructorRequiredStruct.validateNewInstance(missingRequiredFieldInstance).asScala
        result must be(List(requiredErrorMessage))
      }
      "return a MissingConstructionRequiredField when missing a required field" in {
        val result = ConstructorRequiredStruct
          .validateNewInstance(missingConstructionRequiredFieldInstance).asScala
        result must be(List(constructionRequiredErrorMessage))
      }

      "return an empty list when DeepValidationStruct is completely valid" in {
        val struct = buildDeepValidationStruct()
        val result = DeepValidationStruct.validateNewInstance(struct).asScala
        result must be(List.empty)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid requiredConstructorRequiredStruct" in {
        val struct =
          buildDeepValidationStruct(requiredField = missingConstructionRequiredFieldInstance)
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid optionalConstructorRequiredStruct" in {
        val struct = buildDeepValidationStruct(
          optionalField = missingConstructionRequiredFieldInstance
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inList" in {
        val struct = buildDeepValidationStruct(
          listField = List(missingConstructionRequiredFieldInstance).asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inSet" in {
        val struct = buildDeepValidationStruct(
          setField = Set(missingConstructionRequiredFieldInstance).asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inMapKey" in {
        val struct = buildDeepValidationStruct(
          inMapKey = Map(missingConstructionRequiredFieldInstance -> "value").asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inMapValue" in {
        val struct = buildDeepValidationStruct(
          inMapValue = Map("key" -> missingConstructionRequiredFieldInstance).asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid crazyEmbedding" in {
        val struct = buildDeepValidationStruct(
          crazyEmbedding = Map(
            Set(Seq(missingConstructionRequiredFieldInstance).asJava).asJava ->
              Set(Seq(missingConstructionRequiredFieldInstance).asJava).asJava
          ).asJava
        )
        validateMissingConstructionRequiredField(struct, 2)
      }
      "return an empty list when DeepValidationUnion has valid constructorRequiredStruct" in {
        val struct = new DeepValidationUnion()
        struct.setFieldValue(DeepValidationUnion.CONSTRUCTOR_REQUIRED_STRUCT, validInstance)
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List.empty)
      }
      "return a MissingConstructionRequiredField when DeepValidationUnion has invalid constructorRequiredStruct" in {
        val struct = new DeepValidationUnion()
        struct.setFieldValue(
          DeepValidationUnion.CONSTRUCTOR_REQUIRED_STRUCT,
          missingConstructionRequiredFieldInstance
        )
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List(constructionRequiredErrorMessage))
      }
      "return an empty list when DeepValidationUnion has an unrelated field" in {
        val struct = new DeepValidationUnion()
        struct.setFieldValue(DeepValidationUnion.OTHER_FIELD, 1L)
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List.empty)
      }
      "return an error when DeepValidationUnion has no field set" in {
        val struct = new DeepValidationUnion()
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List("No fields set for union type 'DeepValidationUnion'."))
      }
    }
  }

}
