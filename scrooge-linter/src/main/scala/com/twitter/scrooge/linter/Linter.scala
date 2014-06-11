/*
 * Copyright 2014 Twitter, Inc.
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

import com.twitter.finagle.NoStacktrace
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{FileParseException, Importer, ThriftParser}

import java.io.File
import java.util.logging.{Logger, Level}
import scala.collection.mutable.ArrayBuffer

object LintLevel extends Enumeration {
  type LintLevel = Value
  val Warning, Error = Value
}

import LintLevel._

case class LintMessage(msg: String, level: LintLevel = Warning)

trait LintRule extends (Document => Iterable[LintMessage])

object LintRule {
  def all(rules: Seq[LintRule] = DefaultRules): LintRule =
    new LintRule {
      def apply(doc: Document): Seq[LintMessage] =
        rules flatMap { r => r(doc) }
    }

  val DefaultRules = Seq(
    Namespaces,
    RelativeIncludes,
    CamelCase,
    RequiredFieldDefault,
    Keywords
  )

  object Namespaces extends LintRule {
    // All IDLs have a scala and a java namespace
    def apply(doc: Document) = {
      Seq("scala", "java").collect {
        case lang if doc.namespace(lang).isEmpty =>
          LintMessage("Missing namespace: %s.".format(lang), Error)
      }
    }
  }

  object RelativeIncludes extends LintRule {
    // No relative includes
    def apply(doc: Document) = {
      doc.headers.collect {
        case Include(f, d) if f.contains("..") =>
          LintMessage("Relative include path found: %s.".format(f), Error)
      }
    }
  }

  object CamelCase extends LintRule {
    // Struct names are UpperCamelCase.
    // Field names are lowerCamelCase.
    def apply(doc: Document) = {
      val messages = new ArrayBuffer[LintMessage]
      doc.defs.foreach {
        case struct: StructLike =>
          if (!isTitleCase(struct.originalName)) {
            messages += LintMessage("Struct name %s is not UpperCamelCase. Should be: %s.".format(
              struct.originalName, Identifier.toTitleCase(struct.originalName)))
          }

          struct.fields.foreach { f =>
            if (!isCamelCase(f.originalName)) {
              messages += LintMessage("Field name %s.%s is not lowerCamelCase. Should be: %s.".format(
                struct.originalName, f.originalName, Identifier.toCamelCase(f.originalName)))
            }
          }
        case _ =>
      }
      messages
    }

    private[this] def isCamelCase(name: String): Boolean = {
      Identifier.toCamelCase(name) == name
    }
    private[this] def isTitleCase(name: String): Boolean = {
      Identifier.toTitleCase(name) == name
    }
  }

  object RequiredFieldDefault extends LintRule {
    // No default values for required fields
    def apply(doc: Document) = {
      doc.defs.collect {
        case struct: StructLike =>
          struct.fields.collect {
            case f if f.requiredness == Requiredness.Required && f.default.nonEmpty =>
              LintMessage("Required field %s.%s has a default value. Make it optional or remove the default.".format(
                struct.originalName, f.originalName), Error)
          }
      }.flatten
    }
  }

  object Keywords extends LintRule {
    // Struct and field names should not be keywords in Scala, Java, Ruby, Python, PHP.
    def apply(doc: Document) = {
      val messages = new ArrayBuffer[LintMessage]
      val identifiers = doc.defs.collect {
        case struct: StructLike =>
          languageKeywords.foreach { case (lang, keywords) =>
            if (keywords.contains(struct.originalName)) {
              messages += LintMessage(
                "Struct name %s is a %s keyword. Avoid using keywords as identifiers.".format(
                  struct.originalName, lang))
              }
          }
          val fieldNames = struct.fields.map(_.originalName).toSet
          for {
            (lang, keywords) <- languageKeywords
            intersection = keywords.intersect(fieldNames) if intersection.nonEmpty
          } messages += LintMessage("Fields in struct %s: %s are %s keywords. Avoid using keywords as identifiers.".format(
              struct.originalName, intersection.mkString(", "), lang))
      }
      messages
    }

    private[this] val languageKeywords: Map[String, Set[String]] = Map(
      "scala" -> Set("abstract", "case", "catch", "class", "def", "do", "else",
        "extends", "false", "final", "finally", "for", "forSome", "if",
        "implicit", "import", "lazy", "match", "new", "null", "object",
        "override", "package", "private", "protected", "return", "sealed",
        "super", "this", "throw", "trait", "try", "true",
        "type", "val", "var", "while", "with", "yield"),

      "java" -> Set("abstract",
        "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends",
        "final", "finally", "float", "for", "goto", "if", "implements", "import",
        "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while"),

      "ruby" -> Set("BEGIN", "END", "__ENCODING__", "__END__", "__FILE__", "__LINE__",
        "alias", "and", "begin", "break", "case", "class", "def", "defined?",
        "do", "else", "elsif", "end", "ensure", "false", "for", "if",
        "in", "module", "next", "nil", "not", "or", "redo", "rescue", "retry",
        "return", "self", "super", "then", "true", "undef", "unless", "until",
        "when", "while", "yield"),

      "php" -> Set("__halt_compiler", "abstract", "and", "array", "as", "break", "callable",
        "case", "catch", "class", "clone", "const", "continue", "declare", "default",
        "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor",
        "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends",
        "final", "finally", "for", "foreach", "function", "global", "goto", "if",
        "implements", "include", "include_once", "instanceof", "insteadof", "interface",
        "isset", "list", "namespace", "new", "or", "print", "private", "protected",
        "public", "require", "require_once", "return", "static", "switch", "throw",
        "trait", "try", "unset", "use", "var", "while", "xor", "yield"),

      "python" -> Set("and", "as", "assert", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "exec", "finally", "for", "from", "global",
        "if", "import", "in", "is", "lambda", "not", "or", "pass",
        "print", "raise", "return", "try", "while", "with", "yield")
    )

    // Returns a list of languages in which id is a keyword.
    private[this] def checkKeyword(id: String): Iterable[String] = {
      languageKeywords.collect { case (lang, keywords) if keywords.contains(id) =>
        lang
      }
    }
  }
}

object ErrorLogLevel extends Level("LINT-ERROR", 999)
object WarningLogLevel extends Level("LINT-WARN", 998)

case class LintException(msg: String) extends Exception(msg) with NoStacktrace

class Linter(cfg: Config, rules: Seq[LintRule] = LintRule.DefaultRules) {
  def apply(
    doc: Document
  ) {
    val log = Logger.getLogger("linter")

    val messages = LintRule.all(rules)(doc)

    messages.foreach {
      case LintMessage(msg, Error) =>
        log.log(ErrorLogLevel, msg)
      case LintMessage(msg, Warning) =>
        log.log(WarningLogLevel, msg)
    }

    val errorCount = messages.count(_.level == Error)
    val warnCount = messages.count(_.level == Warning)

    if (errorCount + warnCount > 0) {
      log.log(WarningLogLevel, "%d warnings and %d errors found".format(messages.size - errorCount, errorCount))
    }

    if (errorCount > 0) {
      throw new LintException("Lint errors found!")
    }
  }

  def lint() {
    val importer = Importer(new File("."))
    val parser = new ThriftParser(importer, cfg.strict, defaultOptional = false, skipIncludes = true)

    for (inputFile <- cfg.files) {
      if (cfg.verbose) println("+ Linting %s".format(inputFile))

      try {
        val doc0 = parser.parseFile(inputFile)

        apply(doc0)
      } catch {
        case e: FileParseException if (cfg.ignoreErrors) =>
          e.printStackTrace()
        case e: LintException if (cfg.ignoreErrors) =>
          e.printStackTrace()
      }
    }
  }
}
