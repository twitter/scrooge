package com.twitter.scrooge.linter

import com.twitter.app.LoadService
import com.twitter.scrooge.ast._
import scala.collection.mutable.ArrayBuffer

trait LintRule extends (Document => Iterable[LintMessage]) {
  def requiresIncludes: Boolean = false
  def name: String = getClass.getSimpleName.replaceAll("\\p{Punct}", "") // no $
}

object LintRule {
  def all(rules: Seq[LintRule]): LintRule =
    new LintRule {
      def apply(doc: Document): Seq[LintMessage] =
        rules.flatMap { r =>
          r(doc)
        }
    }

  val DefaultRules: Seq[LintRule] = Seq(
    Namespaces,
    CompilerOptimizedMethodParamLimit,
    RelativeIncludes,
    CamelCase,
    RequiredFieldDefault,
    Keywords,
    FieldIndexGreaterThanZeroRule,
    MalformedDocstring,
    MapKeyType
  ) ++ LoadService[LintRule]()

  val Rules: Seq[LintRule] = DefaultRules ++ Seq(
    // Add any optional rules here.
    // These can be enabled with enable-rule flag.
  )

  object Namespaces extends LintRule {
    // All IDLs have a scala and a java namespace
    def apply(doc: Document): Seq[LintMessage] = {
      Seq("scala", "java").collect {
        case lang if doc.namespace(lang).isEmpty =>
          LintMessage("Missing namespace: %s.".format(lang))
      }
    }
  }

  object RelativeIncludes extends LintRule {
    // No relative includes
    def apply(doc: Document): Seq[LintMessage] = {
      doc.headers.collect {
        case include @ Include(f, d) if f.contains("..") =>
          LintMessage(s"Relative include path found:\n${include.pos.longString}")
      }
    }
  }

  object CompilerOptimizedMethodParamLimit extends LintRule {

    /**
     * It turns out that C2 jvm compiler has a limit on the number of fields a method can take.
     * The limit is between 66 and 69.
     *
     * When a method M takes more fields than the limit, then C2 compiler refuses to compile the
     * method and marks the method as not compilable at all tiers. This makes every subsequently
     * compiled call to M a call into the interpreter causing hotspots and high CPU times/throttling.
     */
    protected[linter] val OptimalLimit: Int = 66

    private[this] def lintMessage(
      lintType: String,
      thriftObj: String,
      foundCount: Int
    ): LintMessage = {
      val msg =
        s"""
           |Too many $lintType found in $thriftObj. Optimal compiler limit is $OptimalLimit, but $foundCount items were found.
           | This will generate a method too large for inlining which may lead to higher than expected CPU costs.
        """.stripMargin.replace("\n", "")

      LintMessage(msg, level = Warning)
    }

    def apply(doc: Document): Seq[LintMessage] = {

      // struct fields are generated in an unapply function
      val structs = doc.defs.collect {
        case s: StructLike if s.fields.length >= OptimalLimit & !s.isInstanceOf[Union] =>
          lintMessage("fields for thrift struct", s"${s.originalName} struct", s.fields.length)
      }

      // service function fields are generated as a function
      val serviceFnParams = doc.defs.collect {
        case service: Service =>
          service.functions.collect {
            case fn: Function if fn.args.length >= OptimalLimit =>
              lintMessage(
                "thrift service method parameters",
                s"${service.sid.name}.${fn.funcName.name} function",
                fn.args.length)
          }
      }.flatten

      // service function exceptions fields generate thrift response struct
      // constructors and an unapply function
      val serviceFnExceptions = doc.defs.collect {
        case service: Service =>
          service.functions.collect {
            case fn: Function if fn.throws.length >= OptimalLimit =>
              lintMessage(
                "thrift service method exceptions",
                s"${service.sid.name}.${fn.funcName.name} function",
                fn.throws.length
              )
          }
      }.flatten

      // service functions are generated as constructor parameters for the service
      val serviceFns = doc.defs.collect {
        case service: Service if service.functions.length >= OptimalLimit =>
          lintMessage(
            "thrift service methods",
            s"${service.sid.name} struct",
            service.functions.length)
      }

      structs ++ serviceFns ++ serviceFnParams ++ serviceFnExceptions
    }
  }

  object CamelCase extends LintRule {
    // Struct names are UpperCamelCase.
    // Field names are lowerCamelCase.
    def apply(doc: Document): Seq[LintMessage] = {
      val messages = new ArrayBuffer[LintMessage]
      doc.defs.foreach {
        case struct: StructLike =>
          if (!isTitleCase(struct.originalName)) {
            val correctName = Identifier.toTitleCase(struct.originalName)
            messages += LintMessage(
              s"Struct name ${struct.originalName} is not UpperCamelCase. " +
                s"Should be: $correctName. \n${struct.pos.longString}"
            )
          }

          struct.fields.foreach { f =>
            if (!isCamelCase(f.originalName)) {
              messages += LintMessage(
                s"Field name ${f.originalName} is not lowerCamelCase. " +
                  s"Should be: ${Identifier.toCamelCase(f.originalName)}. \n${f.pos.longString}"
              )
            }
          }
        case _ =>
      }
      messages.toSeq
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
    def apply(doc: Document): Seq[LintMessage] = {
      doc.defs.collect {
        case struct: StructLike =>
          struct.fields.collect {
            case f if f.requiredness == Requiredness.Required && f.default.nonEmpty =>
              LintMessage(
                s"Required field ${f.originalName} has a default value. " +
                  s"Make it optional or remove the default.\n${f.pos.longString}"
              )
          }
      }.flatten
    }
  }

  object Keywords extends LintRule {
    // Struct, field, and function names should not be keywords in Scala, Java, Ruby, Python, PHP, Javascript, and Go.
    def apply(doc: Document): Seq[LintMessage] = {
      val messages = new ArrayBuffer[LintMessage]
      doc.defs.collect {
        case struct: StructLike =>
          languageKeywords.foreach {
            case (lang, keywords) =>
              if (keywords.contains(struct.originalName)) {
                messages += LintMessage(
                  s"Struct name ${struct.originalName}} is a $lang keyword. Avoid using keywords as identifiers.\n" +
                    s"${struct.pos.longString}"
                )
              }
          }

          for {
            (lang, keywords) <- languageKeywords
            fields = struct.fields.filter { f =>
              keywords.contains(f.originalName)
            } if fields.nonEmpty
            fieldNames = fields.map(_.originalName)
          } messages += LintMessage(
            s"Found field names that are $lang keywords: ${fieldNames.mkString(", ")}. " +
              s"Avoid using keywords as identifiers.\n${fields.head.pos.longString}"
          )

        case service: Service =>
          for {
            (lang, keywords) <- languageKeywords
            functions = service.functions.filter { f =>
              keywords.contains(f.originalName)
            } if functions.nonEmpty
            functionNames = functions.map(_.originalName)
          } messages += LintMessage(
            s"Found function names that are $lang keywords: ${functionNames.mkString(", ")}. " +
              s"Avoid using keywords as identifiers.\n"
          )
      }
      messages.toSeq
    }

    private[this] val languageKeywords: Map[String, Set[String]] = Map(
      "scala" -> Set(
        "abstract",
        "case",
        "catch",
        "class",
        "def",
        "do",
        "else",
        "extends",
        "false",
        "final",
        "finally",
        "for",
        "forSome",
        "if",
        "implicit",
        "import",
        "lazy",
        "match",
        "new",
        "null",
        "object",
        "override",
        "package",
        "private",
        "protected",
        "return",
        "sealed",
        "super",
        "this",
        "throw",
        "trait",
        "try",
        "true",
        "type",
        "val",
        "var",
        "while",
        "with",
        "yield"
      ),
      "java" -> Set(
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while"
      ),
      "ruby" -> Set(
        "BEGIN",
        "END",
        "__ENCODING__",
        "__END__",
        "__FILE__",
        "__LINE__",
        "alias",
        "and",
        "begin",
        "break",
        "case",
        "class",
        "def",
        "defined?",
        "do",
        "else",
        "elsif",
        "end",
        "ensure",
        "false",
        "for",
        "if",
        "in",
        "module",
        "next",
        "nil",
        "not",
        "or",
        "redo",
        "rescue",
        "retry",
        "return",
        "self",
        "super",
        "then",
        "true",
        "undef",
        "unless",
        "until",
        "when",
        "while",
        "yield"
      ),
      "php" -> Set(
        "__halt_compiler",
        "abstract",
        "and",
        "array",
        "as",
        "break",
        "callable",
        "case",
        "catch",
        "class",
        "clone",
        "const",
        "continue",
        "declare",
        "default",
        "die",
        "do",
        "echo",
        "else",
        "elseif",
        "empty",
        "enddeclare",
        "endfor",
        "endforeach",
        "endif",
        "endswitch",
        "endwhile",
        "eval",
        "exit",
        "extends",
        "final",
        "finally",
        "for",
        "foreach",
        "function",
        "global",
        "goto",
        "if",
        "implements",
        "include",
        "include_once",
        "instanceof",
        "insteadof",
        "interface",
        "isset",
        "list",
        "namespace",
        "new",
        "or",
        "print",
        "private",
        "protected",
        "public",
        "require",
        "require_once",
        "return",
        "static",
        "switch",
        "throw",
        "trait",
        "try",
        "unset",
        "use",
        "var",
        "while",
        "xor",
        "yield"
      ),
      "python" -> Set(
        "and",
        "as",
        "assert",
        "break",
        "class",
        "continue",
        "def",
        "del",
        "elif",
        "else",
        "except",
        "exec",
        "finally",
        "for",
        "from",
        "global",
        "if",
        "import",
        "in",
        "is",
        "lambda",
        "not",
        "or",
        "pass",
        "print",
        "raise",
        "return",
        "try",
        "while",
        "with",
        "yield"
      ),
      "javascript" -> Set(
        "await",
        "break",
        "case",
        "catch",
        "class",
        "const",
        "continue",
        "debugger",
        "default",
        "delete",
        "do",
        "else",
        "enum",
        "export",
        "extends",
        "finally",
        "for",
        "function",
        "if",
        "implements",
        "import",
        "in",
        "instanceof",
        "interface",
        "let",
        "new",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "static",
        "super",
        "switch",
        "this",
        "throw",
        "try",
        "typeof",
        "var",
        "void",
        "while",
        "with",
        "yield"
      ),
      "go" -> Set(
        "break",
        "case",
        "chan",
        "const",
        "continue",
        "default",
        "defer",
        "else",
        "fallthrough",
        "for",
        "func",
        "go",
        "goto",
        "if",
        "import",
        "interface",
        "map",
        "package",
        "range",
        "return",
        "select",
        "struct",
        "switch",
        "type",
        "var"
      )
    )
  }

  /**
   * all fields must have their field index greater than 0
   */
  object FieldIndexGreaterThanZeroRule extends LintRule {
    def apply(doc: Document): Seq[LintMessage] = {
      doc.defs.collect {
        case struct: StructLike =>
          struct.fields.collect {
            case f if f.index <= 0 =>
              LintMessage(
                s"Non positive field id of ${f.originalName}. Field id should be supplied and must be " +
                  s" greater than zero in struct \n${struct.originalName}",
                Warning
              )
          }
      }.flatten
    }
  }

  object MalformedDocstring extends LintRule {
    // Thrift docstring is invalid
    def apply(doc: Document): Seq[LintMessage] = {
      val docStrings: Seq[String] =
        doc.defs.flatMap {
          case enumField: EnumField =>
            Seq(enumField.docstring)
          case enum: Enum =>
            Seq(enum.docstring)
          case const: ConstDefinition =>
            Seq(const.docstring)
          case struct: StructLike =>
            Seq(struct.docstring) ++
              struct.fields.map(_.docstring)
          case service: Service =>
            Seq(service.docstring) ++
              service.functions.map(_.docstring)
          case _ => Seq.empty
        }.flatten

      verifyDocstring(docStrings)
    }

    private def lintMessage(msg: String): LintMessage = {
      LintMessage(s"Malformed Docstring: $msg. Docstring will be omitted from generated code.")
    }

    private def verifyDocstring(docstrings: Seq[String]): Seq[LintMessage] = {
      docstrings
        .flatMap { docstring: String =>
          if (docstring.replaceFirst("/*", "").contains("/*")) {
            Some(lintMessage("contains extra '/*' in docstring body"))
          } else {
            None
          }
        }
    }
  }

  object MapKeyType extends LintRule {
    // keys in maps should be of the base types rather than structs or containers (https://thrift.apache.org/docs/types)
    def apply(doc: Document): Seq[LintMessage] = {
      doc.defs.flatMap {
        case const: ConstDefinition => checkForMapKeyType(const.sid, const.fieldType).toSeq
        case typedef: Typedef => checkForMapKeyType(typedef.sid, typedef.fieldType).toSeq
        case struct: StructLike =>
          struct.fields.flatMap { field =>
            checkForMapKeyType(field.sid, field.fieldType)
          }
        case _ => Seq.empty
      }
    }

    private def checkForMapKeyType(sid: SimpleID, fieldType: FieldType): Option[LintMessage] = {
      fieldType match {
        case m: MapType =>
          m.keyType match {
            case _: BaseType => None
            case _ =>
              Some(
                LintMessage(
                  s"${sid.name} is a map with a complex key type. " +
                    s"Only base types should be used, see https://thrift.apache.org/docs/types#containers."
                )
              )
          }
        case _ => None
      }
    }
  }
}
