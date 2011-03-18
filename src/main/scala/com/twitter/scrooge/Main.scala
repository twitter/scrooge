package com.twitter.scrooge

import _root_.java.io._
import _root_.scala.collection.mutable
import parser.Parser

object Main {
  var sourcePaths = new mutable.ListBuffer[String]
  var destinationPath = "."
  var sourceFiles = new mutable.ListBuffer[String]

  def usage() {
    println()
    println("usage: scrooge [options] <thrift-files...>")
    println()
    println("options:")
    println("    --scala     generate scala bindings (default)")
    println()
    println("    -s <path>   where to find thrift files [may be used multiple times]")
    println("    -d <path>   where to place generated files")
    println()
    println("example:")
    println("    scrooge --scala -s src/main/thrift -d target/generated Service.thrift")
    println()
  }

  def parseArgs(args: List[String]) {
    args match {
      case Nil =>
      case "--help" :: xs =>
        usage()
        System.exit(0)
      case "--scala" :: xs =>
        // ignore for now.
        parseArgs(xs)
      case "-s" :: path :: xs =>
        sourcePaths += path
        parseArgs(xs)
      case "-d" :: path :: xs =>
        destinationPath = path
        parseArgs(xs)
      case filename :: xs =>
        sourceFiles += filename
        parseArgs(xs)
    }
  }

  private def changeExtension(filename: String, extension: String): String = {
    val n = filename.lastIndexOf('.')
    if (n < 0) {
      filename
    } else {
      filename.substring(0, n)
    } + extension
  }

  def processFile(parser: Parser, filename: String) {
    val outputFilename = changeExtension(filename, ".scala")
    val doc = parser.parseFile(filename)
    val text = ScalaGen(doc)
    val packageFolder = ScalaGen.getPackage(doc).map { _.replaceAll("\\.", "/") }.getOrElse(".")
    val outputFolder = new File(destinationPath, packageFolder)
    outputFolder.mkdirs()
    val outputFile = new File(outputFolder, outputFilename)

/*     [apply] Executing 'thrift' with arguments:
       [apply] '--gen'
       [apply] 'java'
       [apply] '--gen'
       [apply] 'rb'
       [apply] '-o'
       [apply] '/Volumes/twitter/flock/target'
       [apply] ''
       [apply] '/Volumes/twitter/flock/src/main/thrift/Edges.thrift'
       [apply] 
*/
    println("Writing: " + outputFile)
    val out = new FileOutputStream(outputFile)
    out.write(text.getBytes())
    out.close()
  }

  def main(args: Array[String]) {
    sourcePaths += "."
    parseArgs(args.toList)
    if (sourceFiles.size == 0) {
      usage()
      System.exit(1)
    }

    val importer = new Importer(sourcePaths: _*)
    val parser = new Parser(importer)

    for (filename <- sourceFiles) {
      processFile(parser, filename)
    }
  }
}
