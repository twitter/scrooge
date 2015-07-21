package com.twitter.scrooge

import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolException, TType}
import org.apache.thrift.transport.TMemoryBuffer
import org.junit.runner.RunWith
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers

@RunWith(classOf[JUnitRunner])
class ThriftUtilSpec extends FunSuite with Checkers {
  val recognizedTypeCodes =
    Set(
      TType.VOID,
      TType.BOOL,
      TType.BYTE,
      TType.I16,
      TType.I32,
      TType.I64,
      TType.DOUBLE,
      TType.STRING,
      TType.STRUCT,
      TType.MAP,
      TType.SET,
      TType.LIST)

  val unrecognizedTypeCodes = arbitrary[Byte].suchThat(b => !recognizedTypeCodes.contains(b))

  test("throws TProtocolException when typ is unrecognized") {
    check {
      val inProt = new TBinaryProtocol(new TMemoryBuffer(8))
      val outProt = new TBinaryProtocol(new TMemoryBuffer(8))
      forAll (unrecognizedTypeCodes) { typ =>
        val e = intercept[TProtocolException] { ThriftUtil.transfer(outProt, inProt, typ) }
        e.getMessage.contains(typ.toString)
      }
    }
  }
}
