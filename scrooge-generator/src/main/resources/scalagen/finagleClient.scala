package {{package}}

import com.twitter.finagle.SourcedException
import com.twitter.finagle.{RichClientParam, service => ctfs}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.{Protocols, ThriftClientRequest}
import com.twitter.scrooge.{TReusableBuffer, ThriftStruct, ThriftStructCodec}
import com.twitter.util.{Future, Return, Throw, Throwables}
import java.nio.ByteBuffer
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.TMemoryInputTransport
import scala.collection.{Map, Set}
import scala.language.higherKinds

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
class {{ServiceName}}$FinagleClient(
    {{#hasParent}}override {{/hasParent}}val service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
    {{#hasParent}}override {{/hasParent}}val clientParam: RichClientParam)
  extends {{#hasParent}}{{finagleClientParent}}(service, clientParam) with {{/hasParent}}{{ServiceName}}[Future] {

  @deprecated("Use com.twitter.finagle.RichClientParam", "2017-08-16")
  def this(
    service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
    protocolFactory: TProtocolFactory = Protocols.binaryFactory(),
    serviceName: String = "{{ServiceName}}",
    stats: StatsReceiver = NullStatsReceiver,
    responseClassifier: ctfs.ResponseClassifier = ctfs.ResponseClassifier.Default
  ) = this(
    service,
    RichClientParam(
      protocolFactory,
      serviceName,
      clientStats = stats,
      responseClassifier = responseClassifier
    )
  )

  @deprecated("Use com.twitter.finagle.RichClientParam", "2017-08-16")
  def this(
    service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
    protocolFactory: TProtocolFactory,
    serviceName: String,
    stats: StatsReceiver
  ) = this(service, protocolFactory, serviceName, stats, ctfs.ResponseClassifier.Default)

  import {{ServiceName}}._

  {{#hasParent}}override {{/hasParent}}def serviceName: String = clientParam.serviceName
{{^hasParent}}

  private[this] def protocolFactory: TProtocolFactory = clientParam.protocolFactory
  private[this] def maxReusableBufferSize: Int = clientParam.maxThriftBufferSize

  private[this] val tlReusableBuffer = TReusableBuffer(maxThriftBufferSize = maxReusableBufferSize)

  protected def encodeRequest(name: String, args: ThriftStruct) = {
    val memoryBuffer = tlReusableBuffer.get()
    try {
      val oprot = protocolFactory.getProtocol(memoryBuffer)

      oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0))
      args.write(oprot)
      oprot.writeMessageEnd()
      oprot.getTransport().flush()
      val bytes = Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length())
      new ThriftClientRequest(bytes, false)
    } finally {
      tlReusableBuffer.reset()
    }
  }

  protected def decodeResponse[T <: ThriftStruct](resBytes: Array[Byte], codec: ThriftStructCodec[T]) = {
    val iprot = protocolFactory.getProtocol(new TMemoryInputTransport(resBytes))
    val msg = iprot.readMessageBegin()
    try {
      if (msg.`type` == TMessageType.EXCEPTION) {
        val exception = TApplicationException.read(iprot) match {
          case sourced: SourcedException =>
            if (serviceName != "") sourced.serviceName = serviceName
            sourced
          case e => e
        }
        throw exception
      } else {
        codec.decode(iprot)
      }
    } finally {
      iprot.readMessageEnd()
    }
  }

  protected def missingResult(name: String) = {
    new TApplicationException(
      TApplicationException.MISSING_RESULT,
      name + " failed: unknown result"
    )
  }

  protected def setServiceName(ex: Throwable): Throwable =
    if (this.serviceName == "") ex
    else {
      ex match {
        case se: SourcedException =>
          se.serviceName = this.serviceName
          se
        case _ => ex
      }
    }

  // ----- end boilerplate.

{{/hasParent}}
  private[this] def stats: StatsReceiver = clientParam.clientStats
  private[this] def responseClassifier: ctfs.ResponseClassifier = clientParam.responseClassifier

  private[this] val scopedStats = if (serviceName != "") stats.scope(serviceName) else stats
{{#functions}}
  {{>finagleClientFunction}}
{{/function}}
}

