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

import com.twitter.logging.{ConsoleHandler, Formatter}
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{FileParseException, Importer, ThriftParser, TypeResolver}

import java.io.File
import java.util.logging.{Logger, LogRecord, LogManager, Level}

import scala.collection.mutable.ArrayBuffer

object LintLevel extends Enumeration {
  type LintLevel = Value
  val Warning, Error = Value
}

import LintLevel._

case class LintMessage(msg: String, level: LintLevel = Warning)

trait LintRule extends (Document => Iterable[LintMessage]) {
  def requiresIncludes: Boolean = false
  def name = getClass.getSimpleName.replaceAll("\\p{Punct}", "") // no $
}

object LintRule {
  def all(rules: Seq[LintRule]): LintRule =
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

  val Rules = DefaultRules ++ Seq(
    TransitivePersistence,
    DocumentedPersisted
  )

  /**
   *  All structs annotated persisted = "true" refer only to structs that are persisted as well
   */
  object TransitivePersistence extends LintRule {

    override def requiresIncludes: Boolean = true

    def isPersisted(struct: StructLike) =
      struct.annotations.getOrElse("persisted", "false") == "true"

    def apply(doc0: Document) = {
      // resolving ReferenceTypes
      val resolver = TypeResolver()(doc0)
      val doc = resolver.document

      def findUnpersistedStructs(s: StructLike, scopePrefix: Option[SimpleID] = None): Seq[String] = {
        val current =
          if (!isPersisted(s)) Seq(scopePrefix.map(_.name + ".").getOrElse("") + s.sid.name)
          else Seq.empty
        (current ++ findUnpersistedStructsFromFields(s.fields.map(_.fieldType))).distinct
      }

      def findUnpersistedStructsFromFields(fieldTypes: Seq[FieldType]): Seq[String] = {
        fieldTypes.flatMap {
          case StructType(s, scopePrefix) => findUnpersistedStructs(s, scopePrefix) // includes Unions
          case EnumType(enum: Enum, _) => Seq.empty // enums don't have annotations
          case MapType(keyType, valueType, _) => findUnpersistedStructsFromFields(Seq(keyType, valueType))
          case SetType(eltType, _) => findUnpersistedStructsFromFields(Seq(eltType))
          case ListType(eltType, _) => findUnpersistedStructsFromFields(Seq(eltType))
          case _:BaseType => Seq.empty // primitive types
          case _:ReferenceType => // ReferenceTypes have been resolved, this can not happen
            throw new UnsupportedOperationException("There should be no ReferenceType anymore after type resolution")
        }
      }

      for {
        struct <- doc.structs if isPersisted(struct) // structs contains all StructLikes including Structs and Unions
        structChild <- findUnpersistedStructs(struct)
      } yield LintMessage(
          s"struct ${struct.originalName} with persisted annotation refers to struct ${structChild} that is not annotated persisted.",
          Error)
    }
  }

  /**
   * all structs annotated (persisted = "true") must have their fields documented
   */
  object DocumentedPersisted extends LintRule {
    def apply(doc: Document) = {
      val persistedStructs = doc.structs.filter(TransitivePersistence.isPersisted(_))
      val fieldsErrors = for {
        s <- persistedStructs
        field <- s.fields if field.docstring.isEmpty
      } yield LintMessage(s"Missing documentation on field ${field.originalName} in struct ${s.originalName} annotated (persisted = 'true').", Error)
      val structErrors = for {
        s <- persistedStructs if s.docstring.isEmpty
      } yield LintMessage(s"Missing documentation on struct ${s.originalName} annotated (persisted = 'true').", Error)
      structErrors ++ fieldsErrors
    }
  }

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
        case include @ Include(f, d) if f.contains("..") =>
          LintMessage(s"Relative include path found:\n${include.pos.longString}", Error)
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
            val correctName = Identifier.toTitleCase(struct.originalName)
            messages += LintMessage(s"Struct name ${struct.originalName} is not UpperCamelCase. " +
              s"Should be: ${correctName}. \n${struct.pos.longString}")
          }

          struct.fields.foreach { f =>
            if (!isCamelCase(f.originalName)) {
              messages += LintMessage(s"Field name ${f.originalName} is not lowerCamelCase. " +
                s"Should be: ${Identifier.toCamelCase(f.originalName)}. \n${f.pos.longString}")
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
              LintMessage(s"Required field ${f.originalName} has a default value. " +
                s"Make it optional or remove the default.\n${f.pos.longString}",
                Error)
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
                s"Struct name ${struct.originalName}} is a $lang keyword. Avoid using keywords as identifiers.\n" +
                s"${struct.pos.longString}")
              }
          }
          val fieldNames = struct.fields.map(_.originalName).toSet
          for {
            (lang, keywords) <- languageKeywords
            fields = struct.fields.filter { f => keywords.contains(f.originalName) } if fields.nonEmpty
            fieldNames = fields.map(_.originalName)
          } messages += LintMessage(s"Found field names that are $lang keywords: ${fieldNames.mkString(", ")}. " +
            s"Avoid using keywords as identifiers.\n${fields.head.pos.longString}")
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

class Linter(cfg: Config) {
  LogManager.getLogManager.reset

  private[this] val log = Logger.getLogger("linter")
  private[this] val formatter = new Formatter() {
    override def format(record: LogRecord) =
      "%s: %s%s".format(record.getLevel.getName, formatText(record), lineTerminator)
  }

  log.addHandler(new ConsoleHandler(formatter, None))

  private[this] val rules = cfg.enabledRules

  def error(msg: String): Unit = log.log(ErrorLogLevel, msg)
  def warning(msg: String): Unit = {
    if (cfg.showWarnings)
      log.log(WarningLogLevel, msg)
  }

  // Lint a document, returning the number of lint errors found.
  def apply(doc: Document, inputFile: String): Int = {

    val messages = LintRule.all(rules)(doc)

    messages.foreach {
      case LintMessage(msg, Error) =>
        error(s"$inputFile\n$msg")
      case LintMessage(msg, Warning) =>
        warning(s"$inputFile\n$msg")
    }

    val errorCount = messages.count(_.level == Error)
    val warnCount = messages.count(_.level == Warning)

    if (errorCount + warnCount > 0 ) {
      warning("%d warnings and %d errors found".format(messages.size - errorCount, errorCount))
    }
    errorCount
  }

  // Lint cfg.files and return the total number of lint errors found.
  def lint(): Int = {
    val requiresIncludes = rules.exists { _.requiresIncludes }
    val importer = Importer(new File(".")) +: Importer(cfg.includePaths)
    val parser = new ThriftParser(importer, cfg.strict, defaultOptional = false, skipIncludes = !requiresIncludes)

    val errorCounts = cfg.files.map { inputFile =>
      if (cfg.verbose)
        log.info("\n+ Linting %s".format(inputFile))

      try {
        val doc0 = parser.parseFile(inputFile)
        apply(doc0, inputFile)
      } catch {
        case e: FileParseException if (cfg.ignoreParseErrors) =>
          e.printStackTrace()
          0
      }
    }
    errorCounts.sum
  }
}
