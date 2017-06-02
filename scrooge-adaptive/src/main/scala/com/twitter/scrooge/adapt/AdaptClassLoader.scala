package com.twitter.scrooge.adapt

/**
 * Class loader that allows loading classes from bytes at runtime. Used for
 * loading adapted classes for Adaptive Scrooge decoding.
 */
private[adapt] class AdaptClassLoader(parent: ClassLoader) extends ClassLoader(parent) {
  def defineClass(name: String, ba: Array[Byte]): Class[_] = {
    super.defineClass(name, ba, 0, ba.length)
  }
}
