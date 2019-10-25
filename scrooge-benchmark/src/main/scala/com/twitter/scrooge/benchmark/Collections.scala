package com.twitter.scrooge.benchmark

import com.twitter.scrooge.ThriftStructCodec
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.Random
import org.apache.thrift.protocol.{TProtocol, TBinaryProtocol}
import org.apache.thrift.transport.TTransport
import org.openjdk.jmh.annotations._
import thrift.benchmark._
import scala.collection.mutable

private class ExposedBAOS extends ByteArrayOutputStream {
  def get: Array[Byte] = buf
  def len: Int = count
}

class TRewindable extends TTransport {
  private[this] var pos = 0
  private[this] val arr = new ExposedBAOS()

  override def isOpen = true
  override def open(): Unit = {}
  override def close(): Unit = {}
  override def flush(): Unit = {}

  override def read(buf: Array[Byte], off: Int, len: Int): Int = {
    val amtToRead = if (len > arr.len - pos) arr.len - pos else len
    if (amtToRead > 0) {
      System.arraycopy(arr.get, pos, buf, off, amtToRead)
      pos += amtToRead
    }
    amtToRead
  }

  override def write(buf: Array[Byte], off: Int, len: Int): Unit = {
    arr.write(buf, off, len)
  }

  def rewind(): Unit = {
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
  val map: TRewindable = new TRewindable
  val mapProt: TBinaryProtocol = new TBinaryProtocol(map)

  val set: TRewindable = new TRewindable
  val setProt: TBinaryProtocol = new TBinaryProtocol(set)

  val list: TRewindable = new TRewindable
  val listProt: TBinaryProtocol = new TBinaryProtocol(list)

  val rng: Random = new Random(31415926535897932L)

  val mapVals: mutable.Builder[(Long, String), Map[Long, String]] = Map.newBuilder[Long, String]
  val setVals: mutable.Builder[Long, Set[Long]] = Set.newBuilder[Long]
  val listVals: mutable.Builder[Long, Seq[Long]] = Seq.newBuilder[Long]

  val m: Unit = for (_ <- (0 until size)) {
    val num = rng.nextLong()
    mapVals += (num -> num.toString)
    setVals += num
    listVals += num
  }

  MapCollections.encode(MapCollections(mapVals.result), mapProt)
  SetCollections.encode(SetCollections(setVals.result), setProt)
  ListCollections.encode(ListCollections(listVals.result), listProt)

  def run(codec: ThriftStructCodec[_], prot: TProtocol, buff: TRewindable): Unit = {
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
  def timeMap(state: CollectionsState): Unit =
    state.col.run(MapCollections, state.col.mapProt, state.col.map)

  @Benchmark
  def timeSet(state: CollectionsState): Unit =
    state.col.run(SetCollections, state.col.setProt, state.col.set)

  @Benchmark
  def timeList(state: CollectionsState): Unit =
    state.col.run(ListCollections, state.col.listProt, state.col.list)
}
