package com.twitter.scrooge.benchmark

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import java.nio.ByteBuffer
import thrift.benchmark._
import com.twitter.scrooge._

case class StatelessLazyBinaryProtocol[T <: ThriftStruct](codec: ThriftStructCodec[T]) {
  def toBytes(obj: T): Array[Byte] = {
    // These are constructed here for stateless since they are mutable
    // and not thread safe
    val transport = new TArrayByteTransport
    val proto = new TLazyBinaryProtocol(transport)
    codec.encode(obj, proto)
    transport.toByteArray
  }

  def fromBytes(bytes: Array[Byte]): T = {
    val transport = TArrayByteTransport(bytes)
    val proto = new TLazyBinaryProtocol(transport)
    codec.decode(proto)
  }

}

case class ThreadUnsafeLazyBinaryProtocol[T <: ThriftStruct](codec: ThriftStructCodec[T]) {
  val transport = new TArrayByteTransport
  val proto = new TLazyBinaryProtocol(transport)

  def toBytes(obj: T): Array[Byte] = {
    transport.reset()
    codec.encode(obj, proto)
    transport.toByteArray
  }

  def fromBytes(bytes: Array[Byte]): T = {
    transport.setBytes(bytes)
    codec.decode(proto)
  }
}

object LazyTProtocolBenchmark {
  // Pass in a seed and fixed number of airports.
  // Will be initialized with this object so separate from the benchmarks.
  val (airports: Array[Airport], airportBytes) = AirportGenerator.buildAirportsAndBytes(1337, 10)

  // Consume a set of fields
  // lazy deserializers will need to materialize these into the blackhole.
  @inline private final def read3Fields(bh: Blackhole, airport: Airport): Unit = {
    bh.consume(airport.country)
    bh.consume(airport.state)
    bh.consume(airport.loc.map(_.latitude))
  }

  // Consume a set of fields
  // lazy deserializers will need to materialize these into the blackhole.
  @inline private final def readAllFields(bh: Blackhole, airport: Airport): Unit = {
    bh.consume(airport.code)
    bh.consume(airport.name)
    bh.consume(airport.state)
    bh.consume(airport.closestCity)

    if (airport.loc.isDefined) {
      val loc = airport.loc.get
      bh.consume(loc.latitude)
      bh.consume(loc.longitude)
      bh.consume(loc.altitude)
    }
  }

  @State(Scope.Thread)
  class AirportThreadState {
    val threadUnsafeLazySerializer = ThreadUnsafeLazyBinaryProtocol(Airport)
  }

  @State(Scope.Benchmark)
  class AirportState {

    val binaryThriftStructSerializer = BinaryThriftStructSerializer(Airport)

    val statelessLazySerializer = StatelessLazyBinaryProtocol(Airport)

    val statefulLazySerializer = LazyBinaryThriftStructSerializer(Airport)

    @Setup(Level.Trial)
    def setup(): Unit = {
      require(
        airportBytes.forall { b =>
          binaryThriftStructSerializer.fromBytes(b) == statelessLazySerializer.fromBytes(b)
        },
        "Deserializers do not agree, benchmarks pointless"
      )
      require(
        airports.forall { b =>
          ByteBuffer.wrap(binaryThriftStructSerializer.toBytes(b)) == ByteBuffer.wrap(
            statelessLazySerializer.toBytes(b)
          )
        },
        "Stateful vs normal Serializers do not agree, benchmarks pointless"
      )

      require(
        airportBytes.forall { b =>
          binaryThriftStructSerializer.fromBytes(b) == statefulLazySerializer.fromBytes(b)
        },
        "Stateful Deserializers do not agree, benchmarks pointless"
      )
      require(
        airports.forall { b =>
          ByteBuffer.wrap(binaryThriftStructSerializer.toBytes(b)) == ByteBuffer.wrap(
            statefulLazySerializer.toBytes(b)
          )
        },
        "Stateful Serializers do not agree, benchmarks pointless"
      )
    }

  }
}

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class LazyTProtocolBenchmark {
  import LazyTProtocolBenchmark._

  // ========= Reference benchmarks =========

  @Benchmark
  def timeReferenceToBytes(state: AirportState): Seq[Array[Byte]] = {
    airports.map(state.binaryThriftStructSerializer.toBytes)
  }

  @Benchmark
  def timeReferenceFromBytes(state: AirportState): Seq[Airport] = {
    airportBytes.map(state.binaryThriftStructSerializer.fromBytes)
  }

  @Benchmark
  def timeReferenceFromBytesRead3Fields(state: AirportState, bh: Blackhole): Blackhole = {
    airportBytes.map(state.binaryThriftStructSerializer.fromBytes).foreach(a => read3Fields(bh, a))
    bh
  }

  @Benchmark
  def timeReferenceFromBytesReadAllFields(state: AirportState, bh: Blackhole): Blackhole = {
    airportBytes
      .map(state.binaryThriftStructSerializer.fromBytes)
      .foreach(a => readAllFields(bh, a))
    bh
  }

  @Benchmark
  def timeReferenceRTBytes(state: AirportState): Seq[Array[Byte]] = {
    airportBytes.map(
      b =>
        state.binaryThriftStructSerializer.toBytes(state.binaryThriftStructSerializer.fromBytes(b))
    )
  }

  // ========= Stateless benchmarks =========

  @Benchmark
  def timeStatelessToBytes(state: AirportState): Seq[Array[Byte]] = {
    airports.map(state.statelessLazySerializer.toBytes)
  }
  @Benchmark
  def timeStatelessFromBytes(state: AirportState): Seq[Airport] = {
    airportBytes.map(state.statelessLazySerializer.fromBytes)
  }

  @Benchmark
  def timeStatelessFromBytesRead3Fields(state: AirportState, bh: Blackhole): Blackhole = {
    airportBytes.map(state.statelessLazySerializer.fromBytes).foreach(a => read3Fields(bh, a))
    bh
  }

  @Benchmark
  def timeStatelessFromBytesReadAllFields(state: AirportState, bh: Blackhole): Blackhole = {
    airportBytes.map(state.statelessLazySerializer.fromBytes).foreach(a => readAllFields(bh, a))
    bh
  }

  @Benchmark
  def timeStatelessRTBytes(state: AirportState): Seq[Array[Byte]] = {
    airportBytes.map(
      b => state.statelessLazySerializer.toBytes(state.statelessLazySerializer.fromBytes(b))
    )
  }

  // ========= Stateful benchmarks =========

  @Benchmark
  def timeStatefulToBytes(state: AirportState): Seq[Array[Byte]] = {
    airports.map(state.statefulLazySerializer.toBytes)
  }
  @Benchmark
  def timeStatefulFromBytes(state: AirportState): Seq[Airport] = {
    airportBytes.map(state.statefulLazySerializer.fromBytes)
  }

  @Benchmark
  def timeStatefulFromBytesRead3Fields(state: AirportState, bh: Blackhole): Blackhole = {
    airportBytes.map(state.statefulLazySerializer.fromBytes).foreach(a => read3Fields(bh, a))
    bh
  }

  @Benchmark
  def timeStatefulFromBytesReadlAllFields(state: AirportState, bh: Blackhole): Blackhole = {
    airportBytes.map(state.statefulLazySerializer.fromBytes).foreach(a => readAllFields(bh, a))
    bh
  }

  @Benchmark
  def timeStatefulRTBytes(state: AirportState): Seq[Array[Byte]] = {
    airportBytes.map(
      b => state.statefulLazySerializer.toBytes(state.statefulLazySerializer.fromBytes(b))
    )
  }

  // ========= Using the thread state, no built in thread safety =========

  @Benchmark
  def timeThreadUnsafeToBytes(
    state: AirportState,
    threadState: AirportThreadState
  ): Seq[Array[Byte]] = {
    airports.map(threadState.threadUnsafeLazySerializer.toBytes)
  }
  @Benchmark
  def timeThreadUnsafeFromBytes(
    state: AirportState,
    threadState: AirportThreadState
  ): Seq[Airport] = {
    airportBytes.map(threadState.threadUnsafeLazySerializer.fromBytes)
  }

  @Benchmark
  def timeThreadUnsafeFromBytesRead3Fields(
    state: AirportState,
    threadState: AirportThreadState,
    bh: Blackhole
  ): Blackhole = {
    airportBytes
      .map(threadState.threadUnsafeLazySerializer.fromBytes)
      .foreach(a => read3Fields(bh, a))
    bh
  }

  @Benchmark
  def timeThreadUnsafeFromBytesReadlAllFields(
    state: AirportState,
    threadState: AirportThreadState,
    bh: Blackhole
  ): Blackhole = {
    airportBytes
      .map(threadState.threadUnsafeLazySerializer.fromBytes)
      .foreach(a => readAllFields(bh, a))
    bh
  }

  @Benchmark
  def timeThreadUnsafeRTBytes(
    state: AirportState,
    threadState: AirportThreadState
  ): Seq[Array[Byte]] = {
    airportBytes.map(
      b =>
        threadState.threadUnsafeLazySerializer
          .toBytes(threadState.threadUnsafeLazySerializer.fromBytes(b))
    )
  }

}
