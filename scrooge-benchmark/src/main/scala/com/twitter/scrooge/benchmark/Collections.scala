package com.twitter.scrooge.benchmark

import com.twitter.scrooge.ThriftStructCodec
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.Random
import org.apache.thrift.protocol.{TProtocol, TBinaryProtocol}
import org.apache.thrift.transport.TTransport
import org.openjdk.jmh.annotations._
import thrift.benchmark._

private class ExposedBAOS extends ByteArrayOutputStream {
  def get = buf
  def len = count
}

class TRewindable extends TTransport {
  private[this] var pos = 0
  private[this] val arr = new ExposedBAOS()

  override def isOpen = true
  override def open() {}
  override def close() {}
  override def flush() {}

  override def read(buf: Array[Byte], off: Int, len: Int): Int = {
    val amtToRead = if (len > arr.len - pos) arr.len - pos else len
    if (amtToRead > 0) {
      System.arraycopy(arr.get, pos, buf, off, amtToRead)
      pos += amtToRead
    }
    amtToRead
  }

  override def write(buf: Array[Byte], off: Int, len: Int) {
    arr.write(buf, off, len)
  }

  def rewind() {
    pos = 0
  }

  def inspect: String = {
    var buf = ""
    var i = 0
    val bytes = arr.toByteArray()
    bytes foreach { byte =>
      buf += (if (pos == i) "==>" else "") + Integer.toHexString(byte & 0xff) + " "
      i += 1
    }
    buf
  }
}

class Collections(size: Int) {
  val map = new TRewindable
  val mapProt = new TBinaryProtocol(map)

  val set = new TRewindable
  val setProt = new TBinaryProtocol(set)

  val list = new TRewindable
  val listProt = new TBinaryProtocol(list)

  val rng = new Random(31415926535897932L)

  val mapVals = Map.newBuilder[Long, String]
  val setVals = Set.newBuilder[Long]
  val listVals = Seq.newBuilder[Long]

  val m = for (_ <- (0 until size)) {
    val num = rng.nextLong()
    mapVals += (num -> num.toString)
    setVals += num
    listVals += num
  }

  MapCollections.encode(MapCollections(mapVals.result), mapProt)
  SetCollections.encode(SetCollections(setVals.result), setProt)
  ListCollections.encode(ListCollections(listVals.result), listProt)

  def run(codec: ThriftStructCodec[_], prot: TProtocol, buff: TRewindable) {
    codec.decode(prot)
    buff.rewind()
  }
}

object CollectionsBenchmark {
  @State(Scope.Thread)
  class CollectionsState {
    @Param(Array("1", "5", "10", "100", "500", "1000"))
    var size: Int = 1

    var col: Collections = _

    @Setup(Level.Trial)
    def setup(): Unit = {
      col = new Collections(size)
    }

  }
}

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.Throughput))
class CollectionsBenchmark {
  import CollectionsBenchmark._

  @Benchmark
  def timeMap(state: CollectionsState) =
    state.col.run(MapCollections, state.col.mapProt, state.col.map)

  @Benchmark
  def timeSet(state: CollectionsState) =
    state.col.run(SetCollections, state.col.setProt, state.col.set)

  @Benchmark
  def timeList(state: CollectionsState) =
    state.col.run(ListCollections, state.col.listProt, state.col.list)
}
