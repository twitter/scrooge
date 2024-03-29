/*
 * Copyright 2014 Twitter, Inc.
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

import com.twitter.scrooge.ast.Document
import com.twitter.scrooge.frontend.FileParseException
import com.twitter.scrooge.frontend.Importer
import com.twitter.scrooge.frontend.ThriftParser
import java.io.File
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger
import scala.collection.mutable

// these must be objects so we can access this constructor
object ErrorLogLevel extends Level("LINT-ERROR", 999)
object WarningLogLevel extends Level("LINT-WARN", 998)

class Linter(cfg: Config) {
  LogManager.getLogManager.reset()

  private[this] val log = Logger.getLogger("linter")
  private[this] val handler = new ConsoleHandler()
  handler.setFormatter(new Formatter() {
    def format(record: LogRecord): String = {
      s"${record.getLevel.getName}: ${formatMessage(record)}\n"
    }
  })
  log.addHandler(handler)

  private[this] val rules = cfg.enabledRules
  var errMsgs: mutable.Map[String, mutable.Set[String]] = mutable.Map()

  def error(msg: String): Unit = log.log(ErrorLogLevel, s"${Console.RED}$msg")
  def warning(msg: String): Unit = {
    if (cfg.showWarnings)
      log.log(WarningLogLevel, msg)
  }
  def addError(msg: String, inputFile: String): Unit = {
    if (errMsgs.isDefinedAt(msg)) errMsgs(msg) += inputFile
    else errMsgs += (msg -> mutable.Set(inputFile))
    ()
  }

  // Lint a document, returning the number of lint errors found.
  def apply(doc: Document, inputFile: String): Int = {

    val messages = LintRule.all(rules)(doc)

    val errorCount = messages.count(_.level == Error)
    val warnCount = messages.count(_.level == Warning)

    if (cfg.fatalWarnings) {
      val errorAndWarnCount = errorCount + warnCount
      messages.foreach {
        case LintMessage(msg, _) =>
          addError(msg, inputFile)
      }

      if (errorAndWarnCount > 0) {
        warning("%d warnings and %d errors found".format(0, errorAndWarnCount))
      }
      errorAndWarnCount
    } else {
      messages.foreach {
        case LintMessage(msg, Error) =>
          addError(msg, inputFile)
        case LintMessage(msg, Warning) =>
          warning(s"$inputFile\n$msg")
        case _ => ()
      }
      if (errorCount + warnCount > 0) {
        warning("%d warnings and %d errors found".format(warnCount, errorCount))
      }
      errorCount
    }
  }

  // Lint cfg.files and return the total number of lint errors found.
  def lint(): Int = {
    val requiresIncludes = rules.exists { _.requiresIncludes }
    val importer = Importer(new File(".")) +: Importer(cfg.includePaths)
    val parser = new ThriftParser(
      importer,
      cfg.strict,
      defaultOptional = false,
      skipIncludes = !requiresIncludes
    )

    val errorCounts = cfg.files.map { inputFile =>
      if (cfg.verbose)
        log.info("\n+ Linting %s".format(inputFile))

      try {
        val doc0 = parser.parseFile(inputFile)
        apply(doc0, inputFile)
      } catch {
        case e: FileParseException if cfg.ignoreParseErrors =>
          e.printStackTrace()
          0
      }
    }

    if (errMsgs.nonEmpty) {
      error("\nERROR SUMMARY:")
      errMsgs.foreach {
        case (msg, inputFiles) => error(s"${msg}:\n${inputFiles.mkString("\n")}")
      }
    }
    errorCounts.sum
  }
}
