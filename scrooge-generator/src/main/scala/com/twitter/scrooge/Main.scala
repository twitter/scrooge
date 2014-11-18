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

import com.twitter.scrooge.backend.{GeneratorFactory, WithFinagle}
import java.io.File
import java.util.Properties
import scopt.OptionParser

object Main {
  def main(args: Array[String]) {
    val compiler = new Compiler()
    if (!parseOptions(compiler, args)) {
      System.exit(1)
    }
    compiler.run()
  }

  def parseOptions(compiler: Compiler, args: Seq[String]): Boolean = {
    val buildProperties = new Properties
    scala.Option(getClass.getResource("build.properties")) foreach { resource =>
      buildProperties.load(resource.openStream)
    }

    val parser = new OptionParser[Compiler]("scrooge") {
      help("help") text("show this help screen")

      override def showUsageOnError: Boolean = true

      opt[Unit]('V', "version") action { (_, c) => 
        println("scrooge " + buildProperties.getProperty("version", "0.0"))
        println("    build " + buildProperties.getProperty("build_name", "unknown"))
        println("    git revision " + buildProperties.getProperty("build_revision", "unknown"))
        System.exit(0)
        c
      } text("print version and quit")

      opt[Unit]('v', "verbose") action { (_, c) =>
        c.verbose = true
        c
      } text("log verbose messages about progress")

      opt[String]('d', "dest") valueName("<path>") action { (d, c) =>
        c.destFolder = d
        c
      } text("write generated code to a folder (default: %s)".format(compiler.defaultDestFolder))

      opt[String]("import-path") unbounded() valueName("<path>") action { (path, c) =>
        c.includePaths ++= path.split(File.pathSeparator)
        c
      } text("[DEPRECATED] path(s) to search for included thrift files (may be used multiple times)")

      opt[String]('i', "include-path") unbounded() valueName("<path>") action { (path, c) =>
        c.includePaths ++= path.split(File.pathSeparator)
        c
      } text("path(s) to search for included thrift files (may be used multiple times)")

      opt[String]('n', "namespace-map") unbounded() valueName("<oldname>=<newname>") action { (mapping, c) =>
        mapping.split("=") match {
          case Array(from, to) => {
            c.namespaceMappings(from) = to
            c
          }
        }
      } text("map old namespace to new (may be used multiple times)")

      opt[String]("default-java-namespace") unbounded() valueName("<name>") action { (name, c) =>
        c.defaultNamespace = name
        c
      } text("Use <name> as default namespace if the thrift file doesn't define its own namespace. " +
        "If this option is not specified either, then use \"thrift\" as default namespace")

      opt[Unit]("disable-strict") action { (_, c) =>
        c.strict = false
        c
      } text("issue warnings on non-severe parse errors instead of aborting")

      opt[String]("gen-file-map") valueName("<path>") action { (path, c) =>
        c.fileMapPath = Some(path)
        c
      } text("generate map.txt in the destination folder to specify the mapping from input thrift files to output Scala/Java files")

      opt[Unit]("dry-run") action { (_, c) =>
        c.dryRun = true
        c
      } text("parses and validates source thrift files, reporting any errors, but" +
        " does not emit any generated source code.  can be used with " +
        "--gen-file-mapping to get the file mapping")

      opt[Unit]('s', "skip-unchanged") action { (_, c) =>
        c.skipUnchanged = true
        c
      } text("Don't re-generate if the target is newer than the input")

      opt[String]('l', "language") action { (languageString, c) =>
        if (GeneratorFactory.languages.toList contains languageString.toLowerCase) {
          compiler.language = languageString
          c
        } else {
          println("language option %s not supported".format(languageString))
          System.exit(0)
          c
        }
      } text("name of language to generate code in ('experimental-java' and 'scala' are currently supported)")

      opt[String]("experiment-flag") valueName("<flag>") action { (flag, c) =>
        c.experimentFlags += flag
        c
      } text("[EXPERIMENTAL] DO NOT USE FOR PRODUCTION. This is meant only for enabling/disabling features for benchmarking")

      opt[Unit]("scala-warn-on-java-ns-fallback") action { (_, c) =>
        c.scalaWarnOnJavaNSFallback = true
        c
      } text("Print a warning when the scala generator falls back to the java namespace")

      opt[Unit]("finagle") action { (_, c) =>
        c.flags += WithFinagle
        c
      } text("generate finagle classes")

      arg[String]("<files...>") unbounded() action { (files, c) =>
        c.thriftFiles += files
        c
      } text("thrift files to compile")
    }
    parser.parse(args, compiler) map { c =>
      true
    } getOrElse {
      false
    }
  }

  def isUnchanged(file: File, sourceLastModified: Long): Boolean = {
    file.exists && file.lastModified >= sourceLastModified
  }
}
