package com.twitter.scrooge

import scala.util.Random

package object benchmark {
  implicit class RichRandom(val rng: Random) extends AnyVal {
    def nextOptString(len: Int): Option[String] =
      if (rng.nextBoolean) Some(rng.nextString(len)) else None

    def maybeRng[T](fn: Random => T): Option[T] =
      if (rng.nextBoolean) Some(fn(rng)) else None
  }
}
