/*
 * Copyright 2020 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.scrooge

import com.twitter.scrooge.backend.{
  GeneratorFactory,
  WithFinagle,
  WithJavaPassThrough,
  ServiceOption
}
import java.io.File
import java.util.Properties
import scopt.OptionParser

case class ScroogeConfig(
  destFolder: String = ".",
  includePaths: List[String] = List(),
  thriftFiles: List[String] = List(),
  flags: Set[ServiceOption] = Set(),
  namespaceMappings: Map[String, String] = Map(),
  verbose: Boolean = false,
  strict: Boolean = true,
  genAdapt: Boolean = false,
  skipUnchanged: Boolean = false,
  languageFlags: Seq[String] = Seq(),
  fileMapPath: scala.Option[String] = None,
  dryRun: Boolean = false,
  language: String = CompilerDefaults.language,
  defaultNamespace: String = CompilerDefaults.defaultNamespace,
  scalaWarnOnJavaNSFallback: Boolean = false,
  javaSerEnumType: Boolean = false)

object ScroogeOptionParser {

  /** Optionally returns config with parsed values from args.
   * @param  args            command line arguments.
   * @param  defaultConfig   config with configurable defaults that is used to store parsed args.
   */
  def parseOptions(
    args: Seq[String],
    defaultConfig: ScroogeConfig = ScroogeConfig()
  ): Option[ScroogeConfig] = {
    val buildProperties = new Properties
    scala.Option(getClass.getResource("build.properties")) foreach { resource =>
      buildProperties.load(resource.openStream)
    }

    val parser = new OptionParser[ScroogeConfig]("scrooge") {
      help("help").text("show this help screen")

      override def showUsageOnError: Option[Boolean] = Some(true)

      opt[Unit]('V', "version")
        .action { (_, c) =>
          println("scrooge " + buildProperties.getProperty("version", "0.0"))
          println("    build " + buildProperties.getProperty("build_name", "unknown"))
          println("    git revision " + buildProperties.getProperty("build_revision", "unknown"))
          System.exit(0)
          c
        }
        .text("print version and quit")

      opt[Unit]('v', "verbose")
        .action((_, c) => c.copy(verbose = true))
        .text("log verbose messages about progress")

      opt[String]('d', "dest")
        .valueName("<path>")
        .action((d, c) => c.copy(destFolder = d))
        .text("write generated code to a folder (default: %s)".format(defaultConfig.destFolder))

      opt[String]("import-path")
        .unbounded()
        .valueName("<path>")
        .action { (path, c) =>
          val includePaths = path.split(File.pathSeparator) ++: c.includePaths
          c.copy(includePaths = includePaths)
        }
        .text(
          "[DEPRECATED] path(s) to search for included thrift files (may be used multiple times)"
        )

      opt[String]('i', "include-path")
        .unbounded()
        .valueName("<path>")
        .action { (path, c) =>
          val includePaths = path.split(File.pathSeparator) ++: c.includePaths
          c.copy(includePaths = includePaths)
        }
        .text("path(s) to search for included thrift files (may be used multiple times)")

      opt[String]('n', "namespace-map")
        .unbounded()
        .valueName("<oldname>=<newname>")
        .action { (mapping, c) =>
          mapping.split("=") match {
            case Array(from, to) =>
              c.copy(namespaceMappings = c.namespaceMappings + (from -> to))
          }
        }
        .text("map old namespace to new (may be used multiple times)")

      opt[String]("default-java-namespace")
        .unbounded()
        .valueName("<name>")
        .action((name, c) => c.copy(defaultNamespace = name))
        .text(
          "Use <name> as default namespace if the thrift file doesn't define its own namespace. " +
            "If this option is not specified either, then use \"thrift\" as default namespace"
        )

      opt[Unit]("disable-strict")
        .action((_, c) => c.copy(strict = false))
        .text("issue warnings on non-severe parse errors instead of aborting")

      opt[String]("gen-file-map")
        .valueName("<path>")
        .action((path, c) => c.copy(fileMapPath = Some(path)))
        .text(
          "generate map.txt in the destination folder to specify the mapping from input thrift files to output Scala/Java files"
        )

      opt[Unit]("dry-run")
        .action((_, c) => c.copy(dryRun = true))
        .text(
          "parses and validates source thrift files, reporting any errors, but" +
            " does not emit any generated source code.  can be used with " +
            "--gen-file-mapping to get the file mapping"
        )

      opt[Unit]('s', "skip-unchanged")
        .action((_, c) => c.copy(skipUnchanged = true))
        .text("Don't re-generate if the target is newer than the input")

      opt[String]('l', "language")
        .action((languageString, c) => c.copy(language = languageString))
        .validate { language =>
          if (GeneratorFactory.languages.toList contains language.toLowerCase)
            success
          else
            failure("language option %s not supported".format(language))
        }
        .text(
          "name of language to generate code in (currently supported languages: " +
            GeneratorFactory.languages.toList.mkString(", ") + ")"
        )

      opt[Unit]("java-ser-enum-type")
        .action((_, c) => c.copy(javaSerEnumType = true))
        .text("Encode a thrift enum as o.a.t.p.TType.ENUM instead of TType.I32")

      opt[String]("language-flag")
        .valueName("<flag>")
        .unbounded()
        .action { (flag, c) =>
          val languageFlags = c.languageFlags :+ flag
          c.copy(languageFlags = languageFlags)
        }
        .text(
          "Pass arguments to supported language generators"
        )

      opt[Unit]("scala-warn-on-java-ns-fallback")
        .action((_, c) => c.copy(scalaWarnOnJavaNSFallback = true))
        .text("Print a warning when the scala generator falls back to the java namespace")

      opt[Unit]("finagle")
        .action((_, c) => c.copy(flags = c.flags + WithFinagle))
        .text("generate finagle classes")

      opt[Unit]("gen-adapt")
        .action((_, c) => c.copy(genAdapt = true))
        .text("Generate code for adaptive decoding for scala.")

      opt[Unit]("java-passthrough")
        .action((_, c) => c.copy(flags = c.flags + WithJavaPassThrough))
        .text("Generate Java code with PassThrough")

      arg[String]("<files...>")
        .unbounded()
        .action { (files, c) =>
          // append files to preserve the order of thrift files from command line.
          val thriftFiles = c.thriftFiles :+ files
          c.copy(thriftFiles = thriftFiles)
        }
        .text("thrift files to compile")
    }
    parser.parse(args, defaultConfig)
  }
}
