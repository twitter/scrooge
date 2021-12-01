/**
 * Generated by Scrooge
 *   version: ?
 *   rev: ?
 *   built at: ?
 */
package com.twitter.scrooge.test.gold.thriftscala

import com.twitter.finagle.{
  Filter => _,
  Service => _,
  thrift => _,
  _
}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.RichServerParam
import com.twitter.util.Future
import org.apache.thrift.protocol._


@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
class PlatinumService$FinagleService(
  iface: PlatinumService.MethodPerEndpoint,
  serverParam: RichServerParam
) extends GoldService$FinagleService(iface, serverParam) {
  import PlatinumService._

  @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
  def this(
    iface: PlatinumService.MethodPerEndpoint,
    protocolFactory: TProtocolFactory,
    stats: StatsReceiver = NullStatsReceiver,
    maxThriftBufferSize: Int = Thrift.param.maxThriftBufferSize,
    serviceName: String = "PlatinumService"
  ) = this(iface, RichServerParam(protocolFactory, serviceName, maxThriftBufferSize, stats))

  @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
  def this(
    iface: PlatinumService.MethodPerEndpoint,
    protocolFactory: TProtocolFactory,
    stats: StatsReceiver,
    maxThriftBufferSize: Int
  ) = this(iface, protocolFactory, stats, maxThriftBufferSize, "PlatinumService")

  @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
  def this(
    iface: PlatinumService.MethodPerEndpoint,
    protocolFactory: TProtocolFactory
  ) = this(iface, protocolFactory, NullStatsReceiver, Thrift.param.maxThriftBufferSize)

  override def serviceName: String = serverParam.serviceName
  private[this] val filters: Filter = new Filter(serverParam)

  // ---- end boilerplate.

  addService("moreCoolThings", {
    val methodService = new _root_.com.twitter.finagle.Service[MoreCoolThings.Args, MoreCoolThings.SuccessType] {
      def apply(args: MoreCoolThings.Args): Future[MoreCoolThings.SuccessType] = {
        val trace = _root_.com.twitter.finagle.tracing.Trace()
        if (trace.isActivelyTracing) {
          trace.recordRpc("moreCoolThings")
          trace.recordBinary("srv/thrift_endpoint", "com.twitter.scrooge.test.gold.thriftscala.PlatinumService#moreCoolThings()")
        }
        try {
          val request_item = com.twitter.scrooge.test.gold.thriftscala.Request.validateInstanceValue(args.request)
          if (request_item.nonEmpty) throw new com.twitter.scrooge.thrift_validation.ThriftValidationException("moreCoolThings", args.request.getClass, request_item)
        } catch  {
           case _: NullPointerException => ()
        }
        iface.moreCoolThings(args.request)
      }
    }
  
    filters.moreCoolThings.andThen(methodService)
  })
}