package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocol

/**
 * Useful common code for generating templated code out of the thrift AST.
 */
trait Generator {
  class InternalError(description: String) extends Exception(description)

  implicit def string2indent(underlying: String) = new Object {
    def indent(level: Int = 1): String = underlying.split("\\n").map { ("  " * level) + _ }.mkString("\n")
    def indent: String = indent(1)
  }

  implicit def seq2indent(underlying: Seq[String]) = new Object {
    def indent(level: Int = 1): String = underlying.mkString("\n").indent(level)
    def indent: String = indent(1)
  }

  implicit def array2indent(underlying: Array[String]) = new Object {
    def indent(level: Int = 1): String = underlying.mkString("\n").indent(level)
    def indent: String = indent(1)
  }
}
