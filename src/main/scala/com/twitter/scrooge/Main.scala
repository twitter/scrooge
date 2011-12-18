/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  var skipUnchanged = false

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
      opt("s", "skip-unchanged", "Don't re-generate if the target is newer than the input", { skipUnchanged = true; () })

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
      val inputFileDir = new File(inputFile).getParent()
      val importer = Importer.fileImporter(inputFileDir :: importPaths.toList)
      val parser = new ScroogeParser(importer)
      val doc0 = parser.parseFile(inputFile).mapNamespaces(namespaceMappings.toMap)

      val outputFile = outputFilename map { new File(_) } getOrElse {
        val packageDir = new File(destFolder, doc0.scalaNamespace.replace('.', File.separatorChar))
        val baseName = AST.stripExtension(new File(inputFile).getName())
        new File(packageDir, baseName + ".scala")
      }
      val lastModified = importer.lastModified(inputFile).getOrElse(Long.MaxValue)
      if (!(skipUnchanged && isUnchanged(outputFile, lastModified))) {
        if (verbose) println("+ Compiling %s".format(inputFile))
        val doc1 = TypeResolver().resolve(doc0).document
        val gen = new scalagen.ScalaGenerator()
        val content = gen(doc1, flags.toSet)

        Option(outputFile.getParentFile()).map { _.mkdirs() }
        val out = new FileWriter(outputFile)
        out.write(content)
        out.close()
      }
    }
  }

  def isUnchanged(file: File, sourceLastModified: Long): Boolean = {
    file.exists && file.lastModified >= sourceLastModified
  }
}
