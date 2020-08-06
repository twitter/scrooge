package com.twitter.scrooge

/**
 * Metadata for a method for a Thrift service.
 *
 * Comments below will use this example IDL:
 * {{{
 * service ExampleService {
 *   i32 boringMethod(
 *     1: i32 input1,
 *     2: string input2
 *   )
 * }
 * }}}
 */
trait ThriftMethod extends ThriftMethodIface {

  /**
   * A struct wrapping method arguments
   *
   * For Scala generated code this will be a wrapper around all of the inputs.
   * Roughly:
   * {{{
   * class Args(input1: Int, input2: String) extends ThriftStruct
   * }}}
   */
  type Args <: ThriftStruct

  /**
   * The successful return type
   *
   * For Scala generated code this will be the response's type.
   * Roughly:
   * {{{
   * type SuccessType = Int
   * }}}
   */
  type SuccessType

  /** Contains success or thrift application exceptions */
  type Result <: ThriftResponse[SuccessType] with ThriftStruct

  // Note there is some indirection here for `FunctionType`, `ServicePerEndpointServiceType`,
  // and `ReqRepServicePerEndpointServiceType`. This is because for Scala generated with
  // Finagle bindings, these add dependencies on Twitter Util and Finagle.
  // This indirection allows us to sidestep that and keep scrooge-core free of those dependencies.

  /**
   * The type of this method, as a function.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * {{{
   * Function1[Args, Future[Int]]
   * }}}
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   */
  type FunctionType

  /**
   * The type of this method, as a function.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * {{{
   * Function1[scrooge.Request[Args], Future[scrooge.Response[Int]]]
   * }}}
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   */
  type ReqRepFunctionType

  /**
   * The type of this method, as a Finagle `Service` from `Args` to
   * `SuccessType`.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * `Service[Args, SuccessType]`.
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   */
  type ServicePerEndpointServiceType

  /**
   * The type of this method, as a Finagle `Service` from `scrooge.Request[Args]` to
   * `scrooge.Response[SuccessType]`.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * `Service[scrooge.Request[Args], scrooge.Response[SuccessType]]`.
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   */
  type ReqRepServicePerEndpointServiceType

  /**
   * Convert a function implementation of this method into a
   * ServicePerEndpoint Finagle `Service` implementation returning `SuccessType`.
   *
   * For Scala generated code without Finagle bindings, this will not implemented.
   */
  def toServicePerEndpointService(f: FunctionType): ServicePerEndpointServiceType

  /**
   * Convert a function implementation of this method into a
   * ReqRepServicePerEndpoint Finagle `Service` implementation returning
   * `scrooge.Response[SuccessType]`.
   *
   * For Scala generated code without Finagle bindings, this will not implemented.
   */
  def toReqRepServicePerEndpointService(f: ReqRepFunctionType): ReqRepServicePerEndpointServiceType

  /** Thrift annotations (user-defined key-value metadata) on the method */
  def annotations: scala.collection.immutable.Map[String, String]

  /** Codec for the request args */
  def argsCodec: ThriftStructCodec3[Args]

  /** Codec for the response */
  def responseCodec: ThriftStructCodec3[Result]

  /** True for oneway thrift methods */
  def oneway: Boolean
}
