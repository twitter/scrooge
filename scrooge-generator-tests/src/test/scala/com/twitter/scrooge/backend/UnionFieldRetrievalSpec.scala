package com.twitter.scrooge.backend

import com.twitter.scrooge._
import com.twitter.scrooge.testutil.EvalHelper
import com.twitter.scrooge.testutil.JMockSpec
import includes.a.thriftscala._

class UnionFieldRetrievalSpec extends JMockSpec with EvalHelper {
  "UnionFieldRetrievalSpec" should {
    "retrieve field info for a union member class" in { _ =>
      ThriftUnion.fieldInfoForUnionClass(classOf[City.CityState]) must be(
        Some(City.CityState.fieldInfo))
      ThriftUnion.fieldInfoForUnionClass(classOf[City.Zipcode]) must be(
        Some(City.Zipcode.fieldInfo))
    }

    "retrieve no field info for a top-level union class" in { _ =>
      ThriftUnion.fieldInfoForUnionClass(classOf[City]) must be(None)
    }

    "fail to retrieve field info for a non-union class" in { _ =>
      assertThrows[IllegalArgumentException] {
        ThriftUnion.fieldInfoForUnionClass(classOf[Address].asInstanceOf[Class[ThriftUnion]])
      }
    }
  }
}
