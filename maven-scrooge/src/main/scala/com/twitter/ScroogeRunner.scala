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
    print("####################")
    print("####################")
    print("####################")
    print("####################")
    thriftFiles.asScala.map { f =>
      val path = f.getPath
      print("### thrift file " + path)
      compiler.thriftFiles += path }
    thriftIncludes.asScala.map { compiler.importPaths += _.getPath }
    namespaceMappings.asScala.map { e => compiler.namespaceMappings.put(e._1, e._2)}

    compiler.run()
  }
/*
  def compile(log: Log, outputDir: File, thriftFiles: Set[File], thriftIncludes: Set[File], namespaceMappings: Map[String, String], flags: Set[String]) {
    for (inputFile <- thriftFiles.asScala) {
      val inputFileDir = inputFile.getParentFile()
      val allDirs: List[File] = inputFileDir :: thriftIncludes.asScala.toList
      val allDirNames = allDirs.map(_.getCanonicalPath)
      val importer = Importer(allDirNames)
      val parser = new ThriftParser(importer, true)
      val doc0 = parser.parseFile(inputFile.getCanonicalPath).mapNamespaces(namespaceMappings.asScala.toMap)

      //val doc1 = TypeResolver()(doc0).document
      //val gen = new ScalaGenerator()
      val resolvedDoc = TypeResolver()(doc0)
      val doc1 = resolvedDoc.document
      val gen = Generator(Language.Scala, resolvedDoc.resolver.includeMap, "thrift")
      val scroogeOpts = new scala.collection.mutable.HashSet[ServiceOption]
      if (flags.contains("finagle") || flags.contains ("--finagle")) {
        scroogeOpts += WithFinagleService
        scroogeOpts += WithFinagleClient
      }
      if (flags.contains("ostrich") || flags.contains("--ostrich")) {
        scroogeOpts += WithOstrichServer
      }
      log.info("Compiling %s with scrooge".format(inputFile))
      log.info("  flags: %s".format(scroogeOpts))
      log.info("  includes: %s".format(thriftIncludes.asScala.mkString(", ")))
      log.info("  namespace mappings: %s".format(namespaceMappings.asScala.map(e => "%s -> %s".format(e._1, e._2)).mkString(", ")))

      gen(doc1, scroogeOpts.toSet, outputDir)
    }
  }
  */

}