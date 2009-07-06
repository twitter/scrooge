package com.twitter.scrooge

object Main {
  def main(args: Array[String]) {
    println("Hello, world!")

    val importer = new Importer(args(0))
    val doc = (new parser.Parser(importer)).parseFile(args(1))
    println(doc)
    println(ScalaGen(doc))
  }
}
