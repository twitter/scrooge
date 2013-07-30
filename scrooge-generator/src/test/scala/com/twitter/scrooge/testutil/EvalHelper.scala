package com.twitter.scrooge.testutil

import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import org.specs.matcher.Matcher
import org.specs.mock.JMocker
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.ThriftStruct

trait EvalHelper { self: JMocker =>
  case class matchEqualsTField(a: TField) extends Matcher[TField]() {
    def apply(v: => TField) = (
      v.equals(a),
      "%s equals %s".format(v, a),
      "%s does not equal %s".format(v, a)
    )
  }

  case class matchEqualsTList(a: TList) extends Matcher[TList]() {
    def apply(v: => TList) = (v.elemType == a.elemType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTSet(a: TSet) extends Matcher[TSet]() {
    def apply(v: => TSet) = (v.elemType == a.elemType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTMap(a: TMap) extends Matcher[TMap]() {
    def apply(v: => TMap) = (v.keyType == a.keyType && v.valueType == a.valueType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  def equal(a: TField) = will(matchEqualsTField(a))
  def equal(a: TList) = will(matchEqualsTList(a))
  def equal(a: TSet) = will(matchEqualsTSet(a))
  def equal(a: TMap) = will(matchEqualsTMap(a))

  def emptyRead(protocol: TProtocol) {
    one(protocol).readStructBegin()
    one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
    one(protocol).readStructEnd()
  }

  def startRead(protocol: TProtocol, field: TField) {
    one(protocol).readStructBegin()
    one(protocol).readFieldBegin() willReturn field
  }

  def nextRead(protocol: TProtocol, field: TField) {
    one(protocol).readFieldEnd()
    one(protocol).readFieldBegin() willReturn field
  }

  def endRead(protocol: TProtocol) {
    one(protocol).readFieldEnd()
    one(protocol).readFieldBegin() willReturn new TField("stop", TType.STOP, 10)
    one(protocol).readStructEnd()
  }

  def startWrite(protocol: TProtocol, field: TField) {
    val s = capturingParam[TStruct]
    one(protocol).writeStructBegin(s.capture)
    one(protocol).writeFieldBegin(equal(field))
  }

  def nextWrite(protocol: TProtocol, field: TField) {
    one(protocol).writeFieldEnd()
    one(protocol).writeFieldBegin(equal(field))
  }

  def endWrite(protocol: TProtocol) {
    one(protocol).writeFieldEnd()
    one(protocol).writeFieldStop()
    one(protocol).writeStructEnd()
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
