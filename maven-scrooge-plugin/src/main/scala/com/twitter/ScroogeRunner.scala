package com.twitter

import org.apache.maven.plugin.logging.Log
import java.io.File
import java.util.{Map, Set}
import scala.collection.JavaConverters._
import scrooge.backend._
import scrooge.frontend.{ThriftParser, Importer}
import scrooge.{Language, TypeResolver}
import scrooge.Compiler

class ScroogeRunner {

  def compile(
    log: Log,
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    flags: Set[String]
  ) {
    val compiler = new Compiler()
    compiler.strict = true
    compiler.destFolder = outputDir.getPath
    compiler.verbose = true
    thriftFiles.asScala.map { compiler.thriftFiles += _.getPath }
    thriftIncludes.asScala.map { compiler.importPaths += _.getPath }
    namespaceMappings.asScala.map { e => compiler.namespaceMappings.put(e._1, e._2)}

    compiler.run()
  }
}