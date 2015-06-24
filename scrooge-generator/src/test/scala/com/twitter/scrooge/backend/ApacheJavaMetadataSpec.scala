package com.twitter.scrooge.backend

import apache_java_thrift._
import com.twitter.scrooge.testutil.Spec
import org.apache.thrift.protocol.TType
import org.apache.thrift.meta_data._
import scala.collection.JavaConverters._


class ApacheJavaMetadataSpec extends Spec {

  "ApacheJavaMetadata" should {
    "generate struct metadata" should {
      val metadata = 
        FieldMetaData
          .getStructMetaDataMap(classOf[ExceptionsAreStructs])
          .asScala
          .map { case(id, value) => (id.getFieldName, value) }

      "for exception field" in {
        val fieldValueMetaData = metadata("exceptionField").valueMetaData
        
        fieldValueMetaData.`type` must be(TType.STRUCT)
        fieldValueMetaData mustBe a[StructMetaData]
        fieldValueMetaData.asInstanceOf[StructMetaData].structClass must be(classOf[MyException])
      }

      "for struct field" in {
        val fieldValueMetaData = metadata("structField").valueMetaData
        
        fieldValueMetaData.`type` must be(TType.STRUCT)
        fieldValueMetaData mustBe a[StructMetaData]
        fieldValueMetaData.asInstanceOf[StructMetaData].structClass must be(classOf[MyStruct])
      }

      "for list of exceptions field" in {
        val fieldValueMetaData = metadata("exceptionListField").valueMetaData
        
        fieldValueMetaData.`type` must be(TType.LIST)
        fieldValueMetaData mustBe a[ListMetaData]

        val listMetaData = fieldValueMetaData.asInstanceOf[ListMetaData]
        listMetaData.elemMetaData.`type` must be(TType.STRUCT)
        listMetaData.elemMetaData mustBe a[StructMetaData]
        listMetaData.elemMetaData.asInstanceOf[StructMetaData].structClass must be(classOf[MyException])
      }

      "for set of exceptions field" in {
        val fieldValueMetaData = metadata("exceptionSetField").valueMetaData
        
        fieldValueMetaData.`type` must be(TType.SET)
        fieldValueMetaData mustBe a[SetMetaData]

        val setMetaData = fieldValueMetaData.asInstanceOf[SetMetaData]
        setMetaData.elemMetaData.`type` must be(TType.STRUCT)
        setMetaData.elemMetaData mustBe a[StructMetaData]
        setMetaData.elemMetaData.asInstanceOf[StructMetaData].structClass must be(classOf[MyException])
      }

      "for map exception to string field" in {
        val fieldValueMetaData = metadata("exceptionToStringMapField").valueMetaData
        
        fieldValueMetaData.`type` must be(TType.MAP)
        fieldValueMetaData mustBe a[MapMetaData]

        val mapMetaData = fieldValueMetaData.asInstanceOf[MapMetaData]
        mapMetaData.keyMetaData.`type` must be(TType.STRUCT)
        mapMetaData.keyMetaData mustBe a[StructMetaData]
        mapMetaData.keyMetaData.asInstanceOf[StructMetaData].structClass must be(classOf[MyException])
        mapMetaData.valueMetaData.`type` must be(TType.STRING)
      }

      "for map string to exceptions field" in {
        val fieldValueMetaData = metadata("stringToExceptionMapField").valueMetaData
        
        fieldValueMetaData.`type` must be(TType.MAP)
        fieldValueMetaData mustBe a[MapMetaData]

        val mapMetaData = fieldValueMetaData.asInstanceOf[MapMetaData]
        mapMetaData.keyMetaData.`type` must be(TType.STRING)
        mapMetaData.valueMetaData.`type` must be(TType.STRUCT)
        mapMetaData.valueMetaData mustBe a[StructMetaData]
        mapMetaData.valueMetaData.asInstanceOf[StructMetaData].structClass must be(classOf[MyException])
      }
    }
  }
}
