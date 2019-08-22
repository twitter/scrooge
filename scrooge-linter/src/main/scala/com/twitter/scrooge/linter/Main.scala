/*
 * Copyright 2011 Twitter, Inc.
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

package com.twitter.scrooge.linter

import java.io.File
import java.util.Properties
import scopt.OptionParser

case class Config(
  strict: Boolean = false,
  showWarnings: Boolean = false,
  files: Seq[String] = Seq(),
  ignoreErrors: Boolean = false,
  ignoreParseErrors: Boolean = false,
  includePaths: Seq[String] = Seq.empty,
  enabledRules: Seq[LintRule] = LintRule.DefaultRules,
  verbose: Boolean = false,
  fatalWarnings: Boolean = false)

object Main {
  def main(args: Array[String]): Unit = {
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
      help("help") text ("show this help screen")

      override def showUsageOnError: Option[Boolean] = Some(true)

      opt[Unit]('V', "version") text ("print version and quit") action { (_, c) =>
        println("scrooge linter " + buildProperties.getProperty("version", "0.0"))
        println("    build " + buildProperties.getProperty("build_name", "unknown"))
        println("    git revision " + buildProperties.getProperty("build_revision", "unknown"))
        sys.exit()
        c
      }

      opt[Unit]('v', "verbose") action { (_, c) =>
        c.copy(verbose = true)
      } text ("log verbose messages about progress")

      opt[Unit]('i', "ignore-errors") text ("return 0 if linter errors are found. If not set, linter returns 1.") action {
        (_, c) =>
          c.copy(ignoreErrors = true)
      }

      opt[String]('n', "include-path") unbounded () valueName ("<path>") action { (path, c) =>
        c.copy(includePaths = c.includePaths ++ path.split(File.pathSeparator))
      } text ("path(s) to search for included thrift files (may be used multiple times)")

      def findRule(ruleName: String) =
        LintRule.Rules
          .find((r) => r.name == ruleName)
          .getOrElse({
            println(
              s"Unknown rule ${ruleName}. Available: ${LintRule.Rules.map(_.name).mkString(", ")}"
            )
            sys.exit(1)
          })

      def ruleList(rules: Seq[LintRule]) = rules.map(_.name).mkString(", ")

      opt[String]('e', "enable-rule") unbounded () valueName ("<rule-name>") action {
        (ruleName, c) =>
          {
            val rule = findRule(ruleName);
            if (c.enabledRules.contains(rule)) c else c.copy(enabledRules = c.enabledRules :+ rule)
          }
      } text (s"rules to be enabled.\n  Available: ${ruleList(LintRule.Rules)}\n  Default: ${ruleList(
        LintRule.DefaultRules)}")

      opt[String]('d', "disable-rule") unbounded () valueName ("<rule-name>") action {
        (ruleName, c) =>
          {
            c.copy(enabledRules = c.enabledRules.filter(_ != findRule(ruleName)))
          }
      } text ("rules to be disabled.")

      opt[Unit]('p', "ignore-parse-errors") text ("continue if parsing errors are found.") action {
        (_, c) =>
          c.copy(ignoreParseErrors = true)
      }

      opt[Unit]('w', "warnings") text ("show linter warnings (default = False)") action { (_, c) =>
        c.copy(showWarnings = true)
      }

      opt[Unit]("disable-strict") text ("issue warnings on non-severe parse errors instead of aborting") action {
        (_, c) =>
          c.copy(strict = false)
      }

      opt[Unit]("fatal-warnings") text ("convert warnings to errors") action { (_, c) =>
        c.copy(fatalWarnings = true)
      }

      arg[String]("<files...>") unbounded () text ("thrift files to compile") action { (input, c) =>
        c.copy(files = c.files :+ input)
      }
    }

    parser.parse(args, Config()) match {
      case Some(cfg) => cfg
      case None =>
        sys.exit()
    }
  }
}
