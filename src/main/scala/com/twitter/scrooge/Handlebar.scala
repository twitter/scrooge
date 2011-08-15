package com.twitter.scrooge

import java.io.Writer
import org.monkey.mustache._

trait Handlebar[T] extends (T => String) {
}

class HandlebarMustache[T](val mustache: Mustache, val mkDictionary: T => Dictionary)
  extends Eval with Handlebar[T]
{
  def apply(item: T): String = {
    mustache(mkDictionary(item), this)
  }

  def apply(item: T, out: Writer) {
    mustache(mkDictionary(item), this, out)
  }

  /** don't html escape strings */
  override protected def escape(s: String): String = s
}
