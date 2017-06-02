package com.twitter.scrooge.adapt

import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicReferenceArray
import scala.reflect.ClassTag

object Par {
  /**
   * Run f in parallel, on many threads.
   * All threads are blocked until just before running f and let go
   * at the same time.
   * @param parallelism
   * @param f function to run in parallel
   *          f takes as input thread number which is in [0, parallelism).
   *          Note thread number is not thread id.
   */
  def inParallel(parallelism: Int)(f: Int => Unit): Unit = {
    val phaser = new Phaser(1)
    val threads = for (i <- 0 until parallelism) yield {
      phaser.register()
      new Thread {
        override def run(): Unit = {
          phaser.arriveAndAwaitAdvance()
          f(i)
        }
      }
    }
    threads.foreach(_.start)
    phaser.arriveAndDeregister()
    threads.foreach(_.join)
  }

  def calcInParallel[T: ClassTag](parallelism: Int)(f: Int => T): Seq[T] = {
    val resultsAtomic = new AtomicReferenceArray[T](parallelism)
    inParallel(parallelism) { i =>
      resultsAtomic.set(i, f(i))
    }
    val results = new Array[T](parallelism)
    for (i <- 0 until parallelism) {
      results(i) = resultsAtomic.get(i)
    }
    results
  }
}
