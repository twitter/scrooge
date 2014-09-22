package com.twitter.scrooge

import com.twitter.scrooge.testutil.Spec
import org.apache.thrift.protocol.TType
import scala.collection.{Map, Set}
import scrooge.test.annotations.thriftscala._
import thrift.test._

// This is a cross project test and I feel bad for putting it here
// but scrooge-generator already has all the infrastructure to do
// generation and testing and anyway requires scrooge-runtime for
// tests.
class ThriftStructMetaDataSpec extends Spec {
  val metaData = XtructColl.metaData
  val fields = metaData.fields.sortBy(_.id)

  "Provide useful metadata" in {
    val s = XtructColl(Map(1 -> 2L), Seq("test"), Set(10.toByte), 123)

    assert(metaData.codecClass == XtructColl.getClass) // mustEqual doesn't work here
    metaData.structClassName must be("thrift.test.XtructColl")
    metaData.structName must be("XtructColl")
    metaData.structClass must be(classOf[XtructColl])

    val Seq(f1, f2, f3, f4) = fields

    f1.name must be("a_map")
    f1.id must be(1)
    f1.`type` must be(TType.MAP)
    f1.manifest must be(Some(implicitly[Manifest[Map[Int, Long]]]))
    f1.getValue[Map[Int, Long]](s) must be(Map(1 -> 2L))

    f2.name must be("a_list")
    f2.id must be(2)
    f2.`type` must be(TType.LIST)
    f2.manifest must be(Some(implicitly[Manifest[Seq[String]]]))
    f2.getValue[Seq[String]](s) must be(Seq("test"))

    f3.name must be("a_set")
    f3.id must be(3)
    f3.`type` must be(TType.SET)
    f3.manifest must be(Some(implicitly[Manifest[Set[Byte]]]))
    f3.getValue[Set[Byte]](s) must be(Set(10.toByte))

    f4.name must be("non_col")
    f4.id must be(4)
    f4.`type` must be(TType.I32)
    f4.manifest must be(Some(implicitly[Manifest[Int]]))
    f4.getValue[Int](s) must be(123)
  }

  "fieldInfos" in {
    (XtructColl.fieldInfos zip fields).foreach { pairs =>
      val (info, field) = pairs
      info.tfield.name must be(field.name)
      // All of the XtructColl fields are required
      info.isOptional must be(false)
      info.isRequired must be(true)

      field.id match {
        case 1 =>
          info.manifest must be(implicitly[Manifest[Map[Int, Long]]])
          info.keyManifest must be(Some(implicitly[Manifest[Int]]))
          info.valueManifest must be(Some(implicitly[Manifest[Long]]))
        case 2 =>
          info.manifest must be(implicitly[Manifest[Seq[String]]])
          info.keyManifest must be(None)
          info.valueManifest must be(Some(implicitly[Manifest[String]]))
        case 3 =>
          info.manifest must be(implicitly[Manifest[Set[Byte]]])
          info.keyManifest must be(None)
          info.valueManifest must be(Some(implicitly[Manifest[Byte]]))
        case 4 =>
          info.manifest must be(implicitly[Manifest[Int]])
          info.keyManifest must be(None)
          info.valueManifest must be(None)
        case _ =>
          throw new Exception("Unexpected field")
      }
    }

    // All of the OneOfEachOptional fields are optional:
    OneOfEachOptional.fieldInfos.foreach { fieldInfo =>
      fieldInfo.isOptional must be(true)
      fieldInfo.isRequired must be(false)
    }

    //All of the OneOfEachWithDefault fields are default:
    OneOfEachWithDefault.fieldInfos.foreach { fieldInfo =>
      fieldInfo.isRequired must be(false)
      fieldInfo.isOptional must be(false)
    }
  }

  // XtructColl has no annotations:

  "reports no annotations in field infos" in {
    val info = XtructColl.fieldInfos(0)
    info.tfield.name must be("a_map")
    info.typeAnnotations must be(Map.empty[String, String])
    info.fieldAnnotations must be(Map.empty[String, String])
  }

  "reports no struct annotations" in {
    XtructColl.structAnnotations must be(Map.empty[String, String])
  }

  // AnnoStruct has one annotation in each position:

  "reports single annotations in field infos" in {
    val info = AnnoStruct.fieldInfos(0)
    info.tfield.name must be("structField")
    info.typeAnnotations must be(Map(
      "structTypeKey" -> "structTypeValue"
    ))
    info.fieldAnnotations must be(Map(
      "structFieldKey" -> "structFieldValue"
    ))
  }

  "reports single struct annotations" in {
    AnnoStruct.structAnnotations must be(Map(
      "structKey" -> "structValue"
    ))
  }

  // MultiAnnoStruct has two annotations in each position:

  "reports multiple annotations in field infos" in {
    val info = MultiAnnoStruct.fieldInfos(0)
    info.tfield.name must be("multiStructField")
    info.typeAnnotations must be(Map(
      "structTypeKey1" -> "structTypeValue1",
      "structTypeKey2" -> "structTypeValue2"
    ))
    info.fieldAnnotations must be(Map(
      "structFieldKey1" -> "structFieldValue1",
      "structFieldKey2" -> "structFieldValue2"
    ))
  }

  "reports multiple struct annotations" in {
    MultiAnnoStruct.structAnnotations must be(Map(
      "structKey1" -> "structValue1",
      "structKey2" -> "structValue2"
    ))
  }

  // Announion has one annotation in each position - EXCEPT for type annotations.
  // For some reason, the thrift code generator doesn't allow that.
  "reports single union annotations" in {
    AnnoUnion.structAnnotations must be(Map(
      "unionKey" -> "unionValue"
    ))
  }

  "contains union field annotations" in {
    AnnoUnion.UnionFieldFieldManifest must be(manifest[AnnoUnion.UnionField])
    val info = AnnoUnion.UnionField.fieldInfo
    info.tfield.name must be("unionField")
    info.tfield.id must be(1: Short)
    info.typeAnnotations must be(Map.empty[String, String])
    info.fieldAnnotations must be(Map(
      "unionFieldKey" -> "unionFieldValue"
    ))
    info.manifest must be(manifest[AnnoStruct])
    info.isOptional must be(false)
    info.keyManifest must be(None)
    info.valueManifest must be(None)
  }

  "contains union manifest info with field types" {
    {
      MatchingFieldAndStruct.MatchingStructFieldFieldManifest must be(
        manifest[MatchingFieldAndStruct.MatchingStructField])
      val info = MatchingFieldAndStruct.MatchingStructField.fieldInfo
      info.tfield.name must be("matchingStructField")
      info.tfield.id must be(1: Short)
      info.typeAnnotations must be(Map.empty[String, String])
      info.fieldAnnotations must be(Map.empty[String, String])
      info.manifest must be(manifest[MatchingStructField])
      info.isOptional must be(false)
      info.keyManifest must be(None)
      info.valueManifest must be(None)
    }
    {
      MatchingFieldAndStruct.MatchingStructListFieldManifest must be(
        manifest[MatchingFieldAndStruct.MatchingStructList])
      val info = MatchingFieldAndStruct.MatchingStructList.fieldInfo
      info.tfield.name must be("matchingStructList")
      info.tfield.id must be(2: Short)
      info.typeAnnotations must be(Map.empty[String, String])
      info.fieldAnnotations must be(Map.empty[String, String])
      info.manifest must be(manifest[Seq[MatchingStructList]])
      info.isOptional must be(false)
      info.keyManifest must be(None)
      info.valueManifest must be(Some(manifest[MatchingStructList]))
    }
    {
      MatchingFieldAndStruct.MatchingStructSetFieldManifest must be(
        manifest[MatchingFieldAndStruct.MatchingStructSet])
      val info = MatchingFieldAndStruct.MatchingStructSet.fieldInfo
      info.tfield.name must be("matchingStructSet")
      info.tfield.id must be(3: Short)
      info.typeAnnotations must be(Map.empty[String, String])
      info.fieldAnnotations must be(Map.empty[String, String])
      info.manifest must be(manifest[Set[MatchingStructSet]])
      info.isOptional must be(false)
      info.keyManifest must be(None)
      info.valueManifest must be(Some(manifest[MatchingStructSet]))
    }
    {
      MatchingFieldAndStruct.MatchingStructMapFieldManifest must be(
        manifest[MatchingFieldAndStruct.MatchingStructMap])
      val info = MatchingFieldAndStruct.MatchingStructMap.fieldInfo
      info.tfield.name must be("matchingStructMap")
      info.tfield.id must be(4: Short)
      info.typeAnnotations must be(Map.empty[String, String])
      info.fieldAnnotations must be(Map.empty[String, String])
      info.manifest must be(manifest[Map[MatchingStructMap, MatchingStructMap]])
      info.isOptional must be(false)
      info.keyManifest must be(Some(manifest[MatchingStructMap]))
      info.valueManifest must be(Some(manifest[MatchingStructMap]))
    }
    0
  }
}
