package com.twitter.scrooge.benchmark

import com.google.caliper.{SimpleBenchmark, Param}
import com.twitter.app.App
import com.twitter.scrooge.ThriftStructCodec
import java.util.concurrent.atomic.AtomicInteger
import java.util.Random
import org.apache.thrift.protocol.{TProtocol, TBinaryProtocol}
import org.apache.thrift.transport.TTransport
import thrift.benchmark._

class TRewindable extends TTransport {
  private[this] var pos = 0
  private[this] val arr = new java.io.ByteArrayOutputStream() {
    def get = buf
    def len = count
  }

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

  def run(nreps: Int, codec: ThriftStructCodec[_], prot: TProtocol, buff: TRewindable) {
    var i = 0
    while (i < nreps) {
      codec.decode(prot)
      buff.rewind()
      i += 1
    }
  }
}

object CollectionsTest extends App {
  val reps = flag("reps", 10000, "Number of reps to run")
  val size = flag("size", 500, "Size of the collection to run")
  val coll = flag("coll", "map", "Collection type to use")

  var i = 0

  def main() {
    var nreps = reps()
    val col = new Collections(size())

    val (codec, prot, buff) = coll() match {
      case "map" => (MapCollections, col.mapProt, col.map)
      case "set" => (SetCollections, col.setProt, col.set)
      case "list" => (ListCollections, col.listProt, col.list)
    }

    println("Reps: %d, Size: %d, Coll: %s".format(nreps, size(), coll()))

    println(" ==== Warming Up ====")
    col.run(nreps, codec, prot, buff)

    println(" ==== Running ====")
    while (i < nreps) {
      codec.decode(prot)
      buff.rewind()
      i += 1
    }
  }
}

class CollectionsBenchmark extends SimpleBenchmark {
  @Param(Array("1024"))
  private[this] var reps: Int = 1024

  @Param(Array("1", "5", "10", "100", "500", "1000"))
  private[this] var size: Int = 1

  private[this] var col: Collections = _

  override protected def setUp() {
    col = new Collections(size)

    // Warmup objects and such
    col.run(10, MapCollections, col.mapProt, col.map)
    col.run(10, SetCollections, col.setProt, col.set)
    col.run(10, ListCollections, col.listProt, col.list)
  }

  def timeMap(nreps: Int) =
    col.run(nreps, MapCollections, col.mapProt, col.map)

  def timeSet(nreps: Int) =
    col.run(nreps, SetCollections, col.setProt, col.set)

  def timeList(nreps: Int) =
    col.run(nreps, ListCollections, col.listProt, col.list)
}
