package com.twitter.scrooge.testutil

import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.ThriftStruct
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import org.hamcrest.{BaseMatcher, Description}
import org.jmock.Expectations
import org.jmock.AbstractExpectations.{any, returnValue}

trait EvalHelper {
  class TFieldMatcher(obj: TField) extends BaseMatcher[TField] {
    def matches(item: Object): Boolean = {
      // unfortunately it's equals(TField), not equals(Object)
      obj.equals(item.asInstanceOf[TField])
    }

    def describeTo(description: Description): Unit = {
      description.appendValue(obj)
    }
  }

  class TListMatcher(obj: TList) extends BaseMatcher[TList] {
    def matches(item: Object): Boolean = {
      val other = item.asInstanceOf[TList]
      obj.elemType == other.elemType && obj.size == other.size
    }

    def describeTo(description: Description): Unit = {
      description.appendValue(obj)
    }
  }

  class TSetMatcher(obj: TSet) extends BaseMatcher[TSet] {
    def matches(item: Object): Boolean = {
      val other = item.asInstanceOf[TSet]
      obj.elemType == other.elemType && obj.size == other.size
    }

    def describeTo(description: Description): Unit = {
      description.appendValue(obj)
    }
  }

  class TMapMatcher(obj: TMap) extends BaseMatcher[TMap] {
    def matches(item: Object): Boolean = {
      val other = item.asInstanceOf[TMap]
      obj.keyType == other.keyType && obj.valueType == other.valueType && obj.size == other.size
    }

    def describeTo(description: Description): Unit = {
      description.appendValue(obj)
    }
  }

  def fieldEqual(obj: TField) = new TFieldMatcher(obj)
  def listEqual(obj: TList) = new TListMatcher(obj)
  def setEqual(obj: TSet) = new TSetMatcher(obj)
  def mapEqual(obj: TMap) = new TMapMatcher(obj)

  def emptyRead(expectations: Expectations, protocol: TProtocol): Unit = {
    import expectations._
    expectations.oneOf(protocol).readStructBegin()
    expectations.oneOf(protocol).readFieldBegin();
    will(returnValue(new TField("stop", TType.STOP, 10)))
    expectations.oneOf(protocol).readStructEnd()
  }

  def startRead(expectations: Expectations, protocol: TProtocol, field: TField): Unit = {
    import expectations._
    expectations.oneOf(protocol).readStructBegin()
    expectations.oneOf(protocol).readFieldBegin(); will(returnValue(field))
  }

  def nextRead(expectations: Expectations, protocol: TProtocol, field: TField): Unit = {
    import expectations._
    expectations.oneOf(protocol).readFieldEnd()
    expectations.oneOf(protocol).readFieldBegin(); will(returnValue(field))
  }

  def endRead(expectations: Expectations, protocol: TProtocol): Unit = {
    import expectations._
    expectations.oneOf(protocol).readFieldEnd()
    expectations.oneOf(protocol).readFieldBegin();
    will(returnValue(new TField("stop", TType.STOP, 10)))
    expectations.oneOf(protocol).readStructEnd()
  }

  def startWrite(expectations: Expectations, protocol: TProtocol, field: TField): Unit = {
    import expectations._
    expectations.oneOf(protocol).writeStructBegin(`with`(any(classOf[TStruct])))
    expectations.oneOf(protocol).writeFieldBegin(`with`(fieldEqual(field)))
  }

  def nextWrite(expectations: Expectations, protocol: TProtocol, field: TField): Unit = {
    import expectations._
    expectations.oneOf(protocol).writeFieldEnd()
    expectations.oneOf(protocol).writeFieldBegin(`with`(fieldEqual(field)))
  }

  def endWrite(expectations: Expectations, protocol: TProtocol): Unit = {
    expectations.oneOf(protocol).writeFieldEnd()
    expectations.oneOf(protocol).writeFieldStop()
    expectations.oneOf(protocol).writeStructEnd()
  }

  def encodeRequest(name: String, args: ThriftStruct): ThriftClientRequest = {
    val buf = new TMemoryBuffer(512)
    val oprot = new TBinaryProtocol.Factory().getProtocol(buf)

    oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0))
    args.write(oprot)
    oprot.writeMessageEnd()

    val bytes = Arrays.copyOfRange(buf.getArray, 0, buf.length)
    new ThriftClientRequest(bytes, false)
  }

  def encodeResponse(name: String, result: ThriftStruct): Array[Byte] = {
    val buf = new TMemoryBuffer(512)
    val oprot = new TBinaryProtocol.Factory().getProtocol(buf)

    oprot.writeMessageBegin(new TMessage(name, TMessageType.REPLY, 0))
    result.write(oprot)
    oprot.writeMessageEnd()

    Arrays.copyOfRange(buf.getArray, 0, buf.length)
  }

}
