package com.twitter.scrooge

import _root_.java.io._

object Main {
  def main(args: Array[String]) {
    val importer = new Importer(args(0))
    val doc = (new parser.Parser(importer)).parseFile(args(1))
    val text = ScalaGen(doc)
    val out = new FileOutputStream(new File(args(2)))
    out.write(text.getBytes())
    out.close()
  }
}
