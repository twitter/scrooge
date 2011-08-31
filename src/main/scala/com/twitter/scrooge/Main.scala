package com.twitter.scrooge

import java.io.{File, FileWriter}
import net.scala0.cmdline._

object Main {
  object Options extends CommandLineParser {
    val version = Flag("v", "version", "Print version number and quit")
    val help = Flag("?", "help", "Print help and quit")
    val outputDir = StringOption("d", "output-dir", "path", "path of the directory to write files to")
    val outputFile = StringOption("o", "output-file", "filename", "name of file to write output to")
    val importPath = StringOption("i", "import-path", "path", "path-separator separated list of paths")
    val namespaceMappings = StringOption("n", "namespace-mappings", "mappings", "comma-separated list of oldNamespace->newNamespace")
    val inputFiles = NakedArgument("inputFiles", true, true, "The name of the thrift files to process")
    val skip = Flag("s", "skip-unchanged", "Don't re-generate if target is newer than input")
    val versionMode = %(version)
    val helpMode = %(help)
    val genMode = %(importPath.? ~ (outputFile | outputDir).? ~ namespaceMappings.? ~ skip.?, inputFiles)
    val spec = %%(versionMode, helpMode, genMode)
  }

  def main(args: Array[String]) {
    Options(args) match {
      case Left(error) =>
        Console.err.println(error)
        Options.printHelp("scrooge")

      case Right((Options.versionMode, _)) =>
        Console.out.println("Version 1.0")

      case Right((Options.helpMode, _)) =>
        Options.printHelp("scrooge")

      case Right((Options.genMode, cmdLine)) =>
        val importPath = cmdLine(Options.importPath).map(_.split(File.pathSeparator).toSeq).getOrElse(Nil)
        val genOptions = Set[scalagen.ScalaServiceOption](
          scalagen.WithFinagleClient,
          scalagen.WithFinagleService,
          scalagen.WithOstrichServer)
        val namespaceMap = cmdLine(Options.namespaceMappings) map { str =>
          str.split(",").toSeq map {
            _.split("->") match {
              case Array(from, to) => (from, to)
            }
          } toMap
        } getOrElse(Map())
        val skipUnchanged = cmdLine(Options.skip)

        for (inputFile <- cmdLine(Options.inputFiles)) {
          val inputFileDir = new File(inputFile).getParent()
          val importer = Importer.fileImporter(inputFileDir +: importPath)
          val parser = new ScroogeParser(importer)
          val doc0 = parser.parseFile(inputFile).mapNamespaces(namespaceMap)
          val outputFile = cmdLine(Options.outputFile) map { new File(_) } getOrElse {
            val outputDir = cmdLine(Options.outputDir) map { new File(_) } getOrElse { new File(".") }
            val packageDir = new File(outputDir, doc0.scalaNamespace.replace('.', File.separatorChar))
            val baseName = AST.stripExtension(new File(inputFile).getName())
            new File(packageDir, baseName + ".scala")
          }
          val lastModified = importer.lastModified(inputFile).getOrElse(Long.MaxValue)
          if (!(isUnchanged(outputFile, lastModified) && skipUnchanged)) {
            val doc1 = TypeResolver().resolve(doc0).document
            val gen = new scalagen.ScalaGenerator()
            val content = gen(doc1, genOptions)

            outputFile.getParentFile().mkdirs()
            val out = new FileWriter(outputFile)
            out.write(content)
            out.close()
          }
        }
    }
  }

  def isUnchanged(file: File, sourceLastModified: Long): Boolean = {
    file.exists && file.lastModified >= sourceLastModified
  }
}
