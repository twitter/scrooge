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
    val inputFiles = NakedArgument("inputFiles", true, true, "The name of the thrift files to process")
    val versionMode = %(version)
    val helpMode = %(help)
    val genMode = %(importPath.? ~ (outputFile | outputDir).?, inputFiles)
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

        for (inputFile <- cmdLine(Options.inputFiles)) {
          val inputFileDir = new File(inputFile).getParent()
          val importer = Importer.fileImporter(inputFileDir +: importPath)
          val parser = new ScroogeParser(importer)
          val doc = TypeResolver().resolve(parser.parseFile(inputFile)).document
          val gen = new scalagen.ScalaGenerator()
          val content = gen(doc, genOptions)
          val outputFile = cmdLine(Options.outputFile) map { new File(_) } getOrElse {
            val outputDir = cmdLine(Options.outputDir) map { new File(_) } getOrElse { new File(".") }
            val packageDir = new File(outputDir, doc.scalaNamespace.replace('.', File.separatorChar))
            val baseName = AST.stripExtension(new File(inputFile).getName())
            new File(packageDir, baseName + ".scala")
          }
          outputFile.getParentFile().mkdirs()
          val out = new FileWriter(outputFile)
          out.write(content)
          out.close()
        }
    }
  }
}
