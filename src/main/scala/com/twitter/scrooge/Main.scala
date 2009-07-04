package com.twitter.scrooge

object Main {
  def main(args: Array[String]) {
    println("Hello, world!")
    
    val importer = new Importer(args(0))
    println(parser.ScalaGen(parser.ThriftIDL.parse(importer(args(1)))))
  }
}
