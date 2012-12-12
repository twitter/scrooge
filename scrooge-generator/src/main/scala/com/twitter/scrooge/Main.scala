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

import com.twitter.scrooge.backend._
import com.twitter.scrooge.frontend._
import java.io.{FileWriter, File}
import java.util.Properties
import scala.collection.mutable
import scopt.OptionParser

object Language extends Enumeration {
  type Language = Value
  val Scala, Java = Value
}

object Main {
  import Language._
  var destFolder: String = "."
  val importPaths = new mutable.ListBuffer[String]
  val thriftFiles = new mutable.ListBuffer[String]
  val flags = new mutable.HashSet[ServiceOption]
  val namespaceMappings = new mutable.HashMap[String, String]
  var verbose = false
  var strict = true
  var skipUnchanged = false
  var fileMapPath: Option[String] = None
  var fileMapWriter: Option[FileWriter] = None
  var language: Language = Scala
  var defaultNamespace: String = "thrift"

  def main(args: Array[String]) {
    val buildProperties = new Properties
    Option(getClass.getResource("build.properties")) foreach { resource =>
      buildProperties.load(resource.openStream)
    }

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
      opt("n", "namespace-map", "<oldname>=<newname>", "map old namespace to new (may be used multiple times)", { mapping: String =>
        mapping.split("=") match {
          case Array(from, to) => namespaceMappings(from) = to
        }
        ()
      })
      opt(None, "default-java-namespace", "<name>",
      "Use <name> as default namespace if the thrift file doesn't define its own namespace. " +
        "If this option is not specified either, then use \"thrift\" as default namespace",
      { name: String =>
        defaultNamespace = name
      })
      opt("disable-strict", "issue warnings on non-severe parse errors instead of aborting", { strict = false; () })
      opt(None, "gen-file-map", "<path>", "generate map.txt in the destination folder to specify the mapping from input thrift files to output Scala/Java files", { path: String =>
        fileMapPath = Some(path)
        ()
      })
      opt("s", "skip-unchanged", "Don't re-generate if the target is newer than the input", { skipUnchanged = true; () })
      opt("l", "language", "name of language to generate code in ('java' and 'scala' are currently supported)", { languageString: String =>
        languageString.toLowerCase match {
          case "scala" => language = Scala
          case "java" => language = Java
          case _ =>
            println("language option %s not supported".format(languageString))
            System.exit(0)
        }
        ()
      })

      opt("finagle", "generate finagle classes", {
        flags += WithFinagleService
        flags += WithFinagleClient
        ()
      })
      opt("ostrich", "generate ostrich server interface", { flags += WithOstrichServer; () })
      arglist("<files...>", "thrift files to compile", { thriftFiles += _ })
    }
    if (!parser.parse(args)) {
      System.exit(1)
    }

    preCompilation()
    compile()
    postCompilation()
  }

  def preCompilation() {
    // if --gen-file-map is specified, prepare the file.
    fileMapWriter = fileMapPath.map { path =>
      val file = new File(path)
      val dir = file.getParentFile
      if (dir != null && !dir.exists()) {
        dir.mkdirs()
      }
      if (verbose) {
        println("+ Writing file mapping to %s".format(path))
      }
      new FileWriter(file)
    }
  }

  def compile() {
    for (inputFile <- thriftFiles) {
      val importer = Importer(new File(".")) +: Importer(importPaths)
      val parser = new ThriftParser(importer, strict)
      val doc0 = parser.parseFile(inputFile).mapNamespaces(namespaceMappings.toMap)

      if (verbose) println("+ Compiling %s".format(inputFile))
      val resolvedDoc = TypeResolver()(doc0)
      val generator = Generator(language, resolvedDoc.resolver.includeMap, defaultNamespace)
      val generatedFiles =
        generator(resolvedDoc.document, flags.toSet, new File(destFolder)).map { _.getPath }

      if (verbose) {
        println("+ Generated %s".format(generatedFiles.mkString(", ")))
      }
      fileMapWriter.foreach { w =>
        generatedFiles.foreach { path =>
          w.write(inputFile + " -> " + path + "\n")
        }
      }
    }
  }

  def postCompilation() {
    fileMapWriter.foreach { _.close() }

    // we need to clear these options in case Main.main() is called
    // multiple times in the same process.
    destFolder = "."
    importPaths.clear()
    thriftFiles.clear()
    flags.clear()
    namespaceMappings.clear()
    verbose = false
    strict = true
    skipUnchanged = false
    fileMapPath = None
    fileMapWriter = None
  }

  def isUnchanged(file: File, sourceLastModified: Long): Boolean = {
    file.exists && file.lastModified >= sourceLastModified
  }
}
