package com.twitter.scrooge

import java.io.{File, FileWriter}

object Main {
  def main(args: Array[String]) {
    // for now, just read in a thrift file and dump out generated crap.
    if (args.size < 1) {
      println("usage: scrooge <file>  =>  generate crap")
      exit(1)
    }

    val outputFile = if (args.size > 1) Some(new File(args(1))) else None
    val parser = new ScroogeParser(Importer.fileImporter())
    val doc = new TypeResolver()(parser.parseFile(args(0)))
    val gen = new scalagen.ScalaGenerator()
    val content = gen(doc)

    outputFile match {
      case None => println(content)
      case Some(file) =>
        val out = new FileWriter(file)
        out.write(content)
        out.close()
    }
  }
}
