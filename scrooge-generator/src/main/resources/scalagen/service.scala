package {{package}}

import com.twitter.scrooge.{
  LazyTProtocol,
  TFieldBlob, ThriftService, ThriftStruct,
  ThriftStructCodec, ThriftStructCodec3,
  ThriftStructFieldInfo, ThriftResponse, ThriftUtil}
{{#withFinagle}}
import com.twitter.finagle.thrift.{Protocols, ThriftClientRequest, ThriftServiceIface}
import com.twitter.util.Future
{{/withFinagle}}
import java.nio.ByteBuffer
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.TMemoryBuffer
import scala.collection.immutable.{Map => immutable$Map}
import scala.collection.mutable.{
  Builder,
  ArrayBuffer => mutable$ArrayBuffer, Buffer => mutable$Buffer,
  HashMap => mutable$HashMap, HashSet => mutable$HashSet}
import scala.collection.{Map, Set}
import scala.language.higherKinds

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
trait {{ServiceName}}[+MM[_]] {{#genericParent}}extends {{genericParent}} {{/genericParent}}{
{{#genericFunctions}}
  {{>function}}
{{/genericFunctions}}
}


{{docstring}}
object {{ServiceName}} { self =>
{{#withFinagle}}
  case class ServiceIface(
{{#inheritedFunctions}}
      {{funcName}}: com.twitter.finagle.Service[{{ParentServiceName}}.{{funcObjectName}}.Args, {{ParentServiceName}}.{{funcObjectName}}.Result]
{{/inheritedFunctions|,}}
  ) extends {{#parent}}{{parent}}.__ServiceIface
    with {{/parent}}__ServiceIface

  // This is needed to support service inheritance.
  trait __ServiceIface {{#parent}} extends {{parent}}.__ServiceIface {{/parent}} {
{{#asyncFunctions}}
    def {{funcName}}: com.twitter.finagle.Service[self.{{funcObjectName}}.Args, self.{{funcObjectName}}.Result]
{{/asyncFunctions}}
  }

  implicit object ServiceIfaceBuilder
    extends com.twitter.finagle.thrift.ServiceIfaceBuilder[ServiceIface] {
      def newServiceIface(
        binaryService: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
        pf: TProtocolFactory = Protocols.binaryFactory(),
        stats: com.twitter.finagle.stats.StatsReceiver
      ): ServiceIface =
        new ServiceIface(
{{#inheritedFunctions}}
          {{funcName}} = ThriftServiceIface({{ParentServiceName}}.{{funcObjectName}}, binaryService, pf, stats)
{{/inheritedFunctions|,}}
      )
  }

  class MethodIface(serviceIface: __ServiceIface)
    extends {{#parent}}{{parent}}.MethodIface(serviceIface) with {{/parent}}{{ServiceName}}[Future] {
{{#thriftFunctions}}
    private[this] val __{{funcName}}_service =
      ThriftServiceIface.resultFilter(self.{{funcObjectName}}) andThen serviceIface.{{funcName}}
    def {{funcName}}({{fieldParams}}): Future[{{typeName}}] =
      __{{funcName}}_service(self.{{funcObjectName}}.Args({{argNames}})){{^isVoid}}{{/isVoid}}{{#isVoid}}.unit{{/isVoid}}
{{/thriftFunctions}}
  }

  implicit object MethodIfaceBuilder
    extends com.twitter.finagle.thrift.MethodIfaceBuilder[ServiceIface, {{ServiceName}}[Future]] {
    def newMethodIface(serviceIface: ServiceIface): {{ServiceName}}[Future] =
      new MethodIface(serviceIface)
  }

{{/withFinagle}}
{{#thriftFunctions}}
  object {{funcObjectName}} extends com.twitter.scrooge.ThriftMethod {
{{#functionArgsStruct}}
    {{>struct}}
{{/functionArgsStruct}}

    type SuccessType = {{typeName}}
{{#internalResultStruct}}
    {{>struct}}
{{/internalResultStruct}}

    val name = "{{funcName}}"
    val serviceName = "{{ServiceName}}"
    val argsCodec = Args
    val responseCodec = Result
    val oneway = {{is_oneway}}
  }

  // Compatibility aliases.
  val {{funcName}}$args = {{funcObjectName}}.Args
  type {{funcName}}$args = {{funcObjectName}}.Args

  val {{funcName}}$result = {{funcObjectName}}.Result
  type {{funcName}}$result = {{funcObjectName}}.Result

{{/thriftFunctions}}

{{#withFinagle}}
  trait FutureIface extends {{#futureIfaceParent}}{{futureIfaceParent}} with {{/futureIfaceParent}}{{ServiceName}}[Future] {
{{#asyncFunctions}}
    {{>function}}
{{/asyncFunctions}}
  }

  class FinagledClient(
      service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
      protocolFactory: TProtocolFactory = Protocols.binaryFactory(),
      serviceName: String = "{{ServiceName}}",
      stats: com.twitter.finagle.stats.StatsReceiver = com.twitter.finagle.stats.NullStatsReceiver)
    extends {{ServiceName}}$FinagleClient(
      service,
      protocolFactory,
      serviceName,
      stats)
    with FutureIface

  class FinagledService(
      iface: FutureIface,
      protocolFactory: TProtocolFactory)
    extends {{ServiceName}}$FinagleService(
      iface,
      protocolFactory)
{{/withFinagle}}
}
