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

package com.twitter.scrooge.linter

import java.io.File
import java.util.Properties
import scopt.immutable.OptionParser

case class Config(
  strict: Boolean = false,
  showWarnings: Boolean = false,
  files: Seq[String] = Seq(),
  ignoreErrors: Boolean = false,
  ignoreParseErrors: Boolean = false
)


object Main {
  def main(args: Array[String]) {
    val cfg = parseOptions(args)
    val numErrors = new Linter(cfg).lint()
    if (numErrors > 0 && !cfg.ignoreErrors)
      sys.exit(1)
  }

  def parseOptions(args: Seq[String]): Config = {
    val buildProperties = new Properties
    Option(getClass.getResource("build.properties")) foreach { resource =>
      buildProperties.load(resource.openStream)
    }

    val parser = new OptionParser[Config]("scrooge-linter") {
      def options = Seq(
        help("h", "help", "show this help screen"),

        flag("V", "version", "print version and quit") { _ =>
          println("scrooge linter " + buildProperties.getProperty("version", "0.0"))
          println("    build " + buildProperties.getProperty("build_name", "unknown"))
          println("    git revision " + buildProperties.getProperty("build_revision", "unknown"))
          sys.exit()
        },

        flag("i", "ignore-errors", "return 0 if linter errors are found. If not set, linter returns 1.") { cfg =>
          cfg.copy(ignoreErrors = true) },

        flag("p", "ignore-parse-errors", "continue if parsing errors are found.") { cfg =>
          cfg.copy(ignoreParseErrors = true) },

        flag("w", "warnings", "show linter warnings (default = False)") { cfg =>
          cfg.copy(showWarnings = true) },

        opt("disable-strict", "issue warnings on non-severe parse errors instead of aborting")
          { (_, cfg) => cfg.copy(strict = false) },

        arglist("<files...>", "thrift files to compile") { (input, cfg) =>
          cfg.copy(files = cfg.files :+ input)
        }
      )
    }

    parser.parse(args, Config()) match {
      case Some(cfg) => cfg
      case None =>
        sys.exit()
    }
  }
}
