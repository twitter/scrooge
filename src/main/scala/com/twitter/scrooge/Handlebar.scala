package com.twitter.scrooge

import org.monkey.mustache._

class Handlebar[T](val mustache: Mustache, val f: T => Dictionary) extends Eval {
  def apply(item: T): String = {
    mustache(f(item), this)
  }

  /** don't html escape strings */
  override protected def escape(s: String): String = s
}
