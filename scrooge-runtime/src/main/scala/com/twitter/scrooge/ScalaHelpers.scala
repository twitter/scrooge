package com.twitter.scrooge

/**
 * This is a shim to let java code call StatsReceiver#counter (which requires a scala Seq) until
 * finagle 4.0 is released.
 */
object ScalaHelpers {
  def seq(x: String) = Seq(x)
}
