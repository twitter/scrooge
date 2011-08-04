package com.twitter.scrooge

object Main {
  def main(args: Array[String]) {
    // for now, just read in a thrift file and dump out generated crap.
    if (args.size < 1) {
      println("usage: scrooge <file>  =>  generate crap")
      exit(1)
    }

    val parser = new ScroogeParser(Importer.fileImporter())
    val doc = new TypeResolver()(parser.parseFile(args(0)))
    val gen = new scalagen.ScalaGenerator()

    println(gen(doc))
  }
}
