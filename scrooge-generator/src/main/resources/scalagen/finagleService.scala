package {{package}}

import com.twitter.finagle.{SimpleFilter, Thrift, Filter => finagle$Filter, Service => finagle$Service}
import com.twitter.finagle.stats.{Counter, NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.RichServerParam
import com.twitter.scrooge.{TReusableBuffer, ThriftMethod, ThriftStruct}
import com.twitter.util.{Future, Return, Throw, Throwables}
import java.nio.ByteBuffer
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.TMemoryInputTransport
import scala.collection.mutable.{
  ArrayBuffer => mutable$ArrayBuffer, HashMap => mutable$HashMap}
import scala.collection.{Map, Set}

import scala.language.higherKinds

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
class {{ServiceName}}$FinagleService(
  iface: {{ServiceName}}[Future],
  serverParam: RichServerParam
) extends {{finagleServiceParent}}{{#hasParent}}(iface, serverParam){{/hasParent}} {
  import {{ServiceName}}._

  @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
  def this(
    iface: {{ServiceName}}[Future],
    protocolFactory: TProtocolFactory,
    stats: StatsReceiver = NullStatsReceiver,
    maxThriftBufferSize: Int = Thrift.param.maxThriftBufferSize,
    serviceName: String = "{{ServiceName}}"
  ) = this(iface, RichServerParam(protocolFactory, serviceName, maxThriftBufferSize, stats))

  @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
  def this(
    iface: {{ServiceName}}[Future],
    protocolFactory: TProtocolFactory,
    stats: StatsReceiver,
    maxThriftBufferSize: Int
  ) = this(iface, protocolFactory, stats, maxThriftBufferSize, "{{ServiceName}}")

  @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
  def this(
    iface: {{ServiceName}}[Future],
    protocolFactory: TProtocolFactory
  ) = this(iface, protocolFactory, NullStatsReceiver, Thrift.param.maxThriftBufferSize)

  {{#hasParent}}override {{/hasParent}}def serviceName: String = serverParam.serviceName
{{^hasParent}}

  private[this] def protocolFactory: TProtocolFactory = serverParam.protocolFactory
  private[this] def maxReusableBufferSize: Int = serverParam.maxThriftBufferSize
  private[this] def stats: StatsReceiver = serverParam.serverStats

  private[this] val tlReusableBuffer = TReusableBuffer(maxThriftBufferSize = maxReusableBufferSize)

  protected val serviceMap = new mutable$HashMap[String, finagle$Service[(TProtocol, Int), Array[Byte]]]()

  protected def addService(name: String, service: finagle$Service[(TProtocol, Int), Array[Byte]]): Unit = {
    serviceMap(name) = service
  }

  protected def exception(name: String, seqid: Int, code: Int, message: String): Future[Array[Byte]] = {
    try {
      val x = new TApplicationException(code, message)
      val memoryBuffer = tlReusableBuffer.get()
      try {
        val oprot = protocolFactory.getProtocol(memoryBuffer)

        oprot.writeMessageBegin(new TMessage(name, TMessageType.EXCEPTION, seqid))
        x.write(oprot)
        oprot.writeMessageEnd()
        oprot.getTransport().flush()
        Future.value(Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length()))
      } finally {
        tlReusableBuffer.reset()
      }
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  protected def reply(name: String, seqid: Int, result: ThriftStruct): Future[Array[Byte]] = {
    try {
      val memoryBuffer = tlReusableBuffer.get()
      try {
        val oprot = protocolFactory.getProtocol(memoryBuffer)

        oprot.writeMessageBegin(new TMessage(name, TMessageType.REPLY, seqid))
        result.write(oprot)
        oprot.writeMessageEnd()

        Future.value(Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length()))
      } finally {
        tlReusableBuffer.reset()
      }
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  final def apply(request: Array[Byte]): Future[Array[Byte]] = {
    val inputTransport = new TMemoryInputTransport(request)
    val iprot = protocolFactory.getProtocol(inputTransport)

    try {
      val msg = iprot.readMessageBegin()
      val service = serviceMap.get(msg.name)
      service match {
        case _root_.scala.Some(svc) =>
          svc(iprot, msg.seqid)
        case _ =>
          TProtocolUtil.skip(iprot, TType.STRUCT)
          exception(msg.name, msg.seqid, TApplicationException.UNKNOWN_METHOD,
            "Invalid method name: '" + msg.name + "'")
      }
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  private object ThriftMethodStats {
    def apply(stats: StatsReceiver): ThriftMethodStats =
      ThriftMethodStats(
        stats.counter("requests"),
        stats.counter("success"),
        stats.counter("failures"),
        stats.scope("failures")
      )
  }

  private case class ThriftMethodStats(
    requestsCounter: Counter,
    successCounter: Counter,
    failuresCounter: Counter,
    failuresScope: StatsReceiver
  )

  protected def perMethodStatsFilter(
    method: ThriftMethod
  ): SimpleFilter[method.Args, method.SuccessType] = {
    val methodStats = ThriftMethodStats((if (serviceName != "") stats.scope(serviceName) else stats).scope(method.name))
    new SimpleFilter[method.Args, method.SuccessType] {
      def apply(
        args: method.Args,
        service: finagle$Service[method.Args, method.SuccessType]
      ): Future[method.SuccessType] = {
        methodStats.requestsCounter.incr()
        service(args).respond {
          case Return(_) =>
            methodStats.successCounter.incr()
          case Throw(ex) =>
            methodStats.failuresCounter.incr()
            methodStats.failuresScope.counter(Throwables.mkString(ex): _*).incr()
        }
      }
    }
  }
  // ---- end boilerplate.

{{/hasParent}}
{{#methodServices}}
  {{>methodService}}
{{/methodService}}
}

