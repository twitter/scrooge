package com.twitter.scrooge

import java.io.{File, FileWriter}
import java.util.Properties
import scala.collection.mutable
import scopt.OptionParser

object Main {
  var destFolder: String = "."
  val importPaths = new mutable.ListBuffer[String]
  val thriftFiles = new mutable.ListBuffer[String]
  var outputFilename: Option[String] = None
  val flags = new mutable.HashSet[scalagen.ScalaServiceOption]
  val namespaceMappings = new mutable.HashMap[String, String]
  var verbose = false

  def main(args: Array[String]) {
    val buildProperties = new Properties
    buildProperties.load(getClass.getResource("build.properties").openStream)

    val parser = new OptionParser("scrooge") {
      help(None, "help", "show this help screen")
      opt("V", "version", "print version and quit", {
        println("scrooge " + buildProperties.getProperty("version", "0.0"))
        println("    build " + buildProperties.getProperty("build_name", "unknown"))
        println("    git revision " + buildProperties.getProperty("build_revision", "unknown"))
        System.exit(0)
        ()
      })
      opt("v", "verbose", "log verbose messages about progress", { verbose = true; () })
      opt("d", "dest", "<path>",
          "write generated code to a folder (default: %s)".format(destFolder), { x: String =>
        destFolder = x
      })
      opt("i", "import-path", "<path>", "path(s) to search for imported thrift files (may be used multiple times)", { path: String =>
        importPaths ++= path.split(File.pathSeparator); ()
      })
      opt("o", "output-file", "<filename>", "name of the file to write generated code to", { filename: String =>
        outputFilename = Some(filename); ()
      })
      opt("n", "namespace-map", "<oldname>=<newname>", "map old namespace to new (may be used multiple times)", { mapping: String =>
        mapping.split("=") match {
          case Array(from, to) => namespaceMappings(from) = to
        }
        ()
      })
      opt("finagle", "generate finagle classes", {
        flags += scalagen.WithFinagleService
        flags += scalagen.WithFinagleClient
        ()
      })
      opt("ostrich", "generate ostrich server interface", { flags += scalagen.WithOstrichServer; () })
      arglist("<files...>", "thrift files to compile", { thriftFiles += _ })
    }
    if (!parser.parse(args)) {
      System.exit(1)
    }

    for (inputFile <- thriftFiles) {
      if (verbose) println("+ Compiling %s".format(inputFile))
      val inputFileDir = new File(inputFile).getParent()
      val importer = Importer.fileImporter(inputFileDir :: importPaths.toList)
      val scrooge = new ScroogeParser(importer)
      val doc = TypeResolver().resolve(scrooge.parseFile(inputFile)).document.mapNamespaces(namespaceMappings.toMap)
      val gen = new scalagen.ScalaGenerator()
      val content = gen(doc, flags.toSet)

      val outputFile = outputFilename map { new File(_) } getOrElse {
        val packageDir = new File(destFolder, doc.scalaNamespace.replace('.', File.separatorChar))
        val baseName = AST.stripExtension(new File(inputFile).getName())
        new File(packageDir, baseName + ".scala")
      }
      Option(outputFile.getParentFile()).map { _.mkdirs() }
      val out = new FileWriter(outputFile)
      out.write(content)
      out.close()
    }
  }
}
