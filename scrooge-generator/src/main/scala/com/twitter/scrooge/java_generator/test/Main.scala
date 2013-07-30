package com.twitter.scrooge.java_generator.test

import com.twitter.scrooge.Compiler
import org.codehaus.plexus.util.FileUtils
import java.io._
import scala.collection.JavaConversions._
import scala.collection.mutable
import org.apache.commons.cli.{PosixParser, Options}

/**
 * Helper class to do side-by-side comparisons on what maven-finagle-thrift plugin generates.
 */
object Main {
  def main(args: Array[String]) {
    val options = new Options
    options.addOption("t", "thrift", true, "thrift resources directory")
    options.addOption("f", "file", true, "thrift file to build")
    options.addOption("g", "apache-generated", true, "apache generated thrift directory")
    options.addOption("d", "dest", true, "scrooge generated thrift destination")
    options.addOption("i", "includes", true, "included thrift files")
    val parser = new PosixParser
    val cmdLine = parser.parse(options, args)

    val originalGen = cmdLine.getOptionValue("g")

    val destDir = cmdLine.getOptionValue("d")
    FileUtils.cleanDirectory(destDir)
    val compiler = new Compiler()
    compiler.destFolder = destDir
    compiler.language = "java"

    if (cmdLine.hasOption("i")) {
      compiler.includePaths += cmdLine.getOptionValue("i")
    }
    if (cmdLine.hasOption("t")) {
      FileUtils.getFiles(new File(cmdLine.getOptionValue("t")), "**/*.thrift", "") foreach { s =>
        compiler.thriftFiles += s.asInstanceOf[File].getAbsolutePath
      }
    }
    if (cmdLine.hasOption("f")) {
      cmdLine.getOptionValues("f").foreach { f =>
        compiler.thriftFiles += f
      }
    }
    compiler.run()

    val newGenMap = new mutable.HashMap[String, File]
    val oldGenMap = new mutable.HashMap[String, File]
    if (newGenMap.size != oldGenMap.size) {
      println("Wrong number of files generated")
      exit(1)
    }
    FileUtils.getFiles(new File(compiler.destFolder), "**/*.java", "") foreach { s =>
      val file = s.asInstanceOf[File]
      newGenMap.put(file.getName, file)
    }
    FileUtils.getFiles(new File(originalGen), "**/*.java", "") foreach { s =>
      val file = s.asInstanceOf[File]
      oldGenMap.put(file.getName, file)
    }

    val success = new mutable.ListBuffer[String]()
    val failed = new mutable.ListBuffer[String]()
    newGenMap map {
      case (k, v) => {
        // println("Comparing: " + v + " and " + oldGenMap(k))
        if (!verify(getFileContents(v), getFileContents(oldGenMap(k)))) {
          failed.add(k)
          println("FAILED: " + v + " and " + oldGenMap(k))
          // System.exit(1)
        } else {
          success.add(k)
        }
      }
    }

    println("Total Success: " + success.size + ", Failures: " + failed.size)
  }

  def verify(actual: String, expected: String): Boolean = {
    val actualItems = ApacheCompatibilityHelpers.cleanWhitespace(actual, true)
    val expectedItems = ApacheCompatibilityHelpers.cleanWhitespace(expected, true)
    for (i <- 0 until actualItems.size) {
      if (!actualItems(i).equals(expectedItems(i))) {
        println("Actual: " + actualItems(i))
        println("Expected: " + expectedItems(i))
        return false
      } else {
        // println(expectedItems(i))
      }
    }
    true
  }

  def getFileContents(file: File): String = {
    try {
      val contents = new StringBuilder
      val input = new BufferedReader(new FileReader(file))
      var line: String = input.readLine
      while (line != null) {
        contents.append(line)
        line = input.readLine
        if (line != null) {
          contents.append(System.getProperty("line.separator"))
        }
      }
      contents.toString()
    } catch {
      case e: FileNotFoundException => {
        throw new RuntimeException("File not found: " + file)
      }
    }
  }
}
