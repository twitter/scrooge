package com.twitter.scrooge.benchmark

import com.twitter.scrooge.TUnboundedByteArrayOutputStream
import java.util.concurrent.TimeUnit
import org.apache.thrift.TByteArrayOutputStream
import org.openjdk.jmh.annotations._

object TByteArrayOutputStreamBenchmark {
  @State(Scope.Thread)
  class InputState {
    val bytes: Int = 16
    val buf: Array[Byte] = new Array[Byte](bytes)
  }

  @State(Scope.Benchmark)
  class TByteArrayOutputStreamState {
    @Param(Array("1024", "10240", "51200"))
    var initialSize: Int = 1024

    var stream: TByteArrayOutputStream = _

    @Setup(Level.Trial)
    def setup(): Unit = {
      stream = new TByteArrayOutputStream(initialSize)
    }
  }

  @State(Scope.Benchmark)
  class TUnboundedByteArrayOutputStreamState {
    @Param(Array("1024", "10240", "51200"))
    var initialSize: Int = 1024

    var stream: TByteArrayOutputStream = _

    @Setup(Level.Trial)
    def setup(): Unit = {
      stream = new TUnboundedByteArrayOutputStream(initialSize)
    }
  }
}

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class TByteArrayOutputStreamBenchmark {
  import TByteArrayOutputStreamBenchmark._

  def run(in: Array[Byte], out: TByteArrayOutputStream): Unit = {
    out.write(in, 0, in.size)
    out.reset()
  }

  @Benchmark
  def timeTByteArrayOutputStream(in: InputState, out: TByteArrayOutputStreamState): Unit =
    run(in.buf, out.stream)

  @Benchmark
  def timeTUnboundedByteArrayOutputStream(
    in: InputState,
    out: TUnboundedByteArrayOutputStreamState
  ): Unit =
    run(in.buf, out.stream)
}
