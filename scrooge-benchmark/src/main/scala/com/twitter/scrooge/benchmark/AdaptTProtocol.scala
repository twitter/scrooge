package com.twitter.scrooge.benchmark

import com.twitter.scrooge.BinaryThriftStructSerializer
import com.twitter.scrooge.adapt.testutil.ReloadOnceAdaptBinarySerializer
import com.twitter.scrooge.benchmark.AdaptTProtocolBenchmark.AirlineThreadState
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import thrift.benchmark._

object AdaptTProtocolBenchmark {
  // Pass in a seed and fixed number of airlines.
  // Will be initialized with this object so separate from the benchmarks.
  val (airlines: Array[Airline], airlinesBytes) =
    AirlineGenerator.buildAirlinesAndBytes(1337, 10)

  // Consume a set of fields
  // lazy deserializers will need to materialize these into the blackhole.
  private final def read3Fields(bh: Blackhole, airline: Airline): Unit = {
    bh.consume(airline.name)
    bh.consume(airline.headQuarter)
    bh.consume(airline.owner)
  }

  @State(Scope.Thread)
  class AirlineThreadState {
    val threadUnsafeLazySerializer = ThreadUnsafeLazyBinaryProtocol(Airline)
    val eagerThriftSerializer = BinaryThriftStructSerializer(Airline)
    val adaptSerializer =
      ReloadOnceAdaptBinarySerializer(Airline)
    var iter = 0
  }
}

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class AdaptTProtocolBenchmark {
  import AdaptTProtocolBenchmark._

  // todo: Add benchmarks for toBytes, all fields accessed, more fields accessed etc.

  @Benchmark
  def timeFromBytesAdapt3FieldsAccessed(threadState: AirlineThreadState, bh: Blackhole): Unit = {
    val bytes = airlinesBytes(threadState.iter % airlines.size)
    threadState.iter += 1
    val airline = threadState.adaptSerializer.fromBytes(bytes)
    read3Fields(bh, airline)
  }

  @Benchmark
  def timeThreadUnsafeFromBytesLazy3FieldsAccessed(
    threadState: AirlineThreadState,
    bh: Blackhole
  ): Unit = {
    val bytes = airlinesBytes(threadState.iter % airlines.size)
    threadState.iter += 1
    val airline = threadState.threadUnsafeLazySerializer.fromBytes(bytes)
    read3Fields(bh, airline)
  }

  @Benchmark
  def timeEagerFromBytesLazy3FieldsAccessed(
    threadState: AirlineThreadState,
    bh: Blackhole
  ): Unit = {
    val bytes = airlinesBytes(threadState.iter % airlines.size)
    threadState.iter += 1
    val airline = threadState.eagerThriftSerializer.fromBytes(bytes)
    read3Fields(bh, airline)
  }
}
