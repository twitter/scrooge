package com.twitter.scrooge.frontend

/**
 *
 * Set of Thrift keywords so we can raise errors or warnings when used in thrift files
 *
 * Many of these keywords are reserved in alignment with ApacheThrift
 * See https://github.com/apache/thrift/blob/master/doc/specs/idl.md#reserved-keywords
 */
private object ThriftKeywords {
  private[this] val set = Set[String](
    "abstract",
    "alias",
    "and",
    "args",
    "as",
    "assert",
    "async",
    "begin",
    "break",
    "case",
    "catch",
    "class",
    "clone",
    "continue",
    "const",
    "declare",
    "def",
    "default",
    "del",
    "delete",
    "do",
    "dynamic",
    "elif",
    "else",
    "elseif",
    "elsif",
    "end",
    "enddeclare",
    "endfor",
    "endforeach",
    "endif",
    "endswitch",
    "endwhile",
    "ensure",
    "enum",
    "except",
    "exec",
    "exception",
    "extends",
    "finally",
    "float",
    "for",
    "foreach",
    "from",
    "function",
    "global",
    "goto",
    "if",
    "implements",
    "import",
    "in",
    "include",
    "inline",
    "instanceof",
    "interface",
    "is",
    "lambda",
    "module",
    "namespace",
    "native",
    "new",
    "next",
    "nil",
    "not",
    "optional",
    "or",
    "package",
    "pass",
    "public",
    "print",
    "private",
    "protected",
    "raise",
    "redo",
    "rescue",
    "retry",
    "register",
    "return",
    "required",
    "self",
    "service",
    "sizeof",
    "static",
    "struct",
    "super",
    "switch",
    "synchronized",
    "then",
    "this",
    "throw",
    "throws",
    "transient",
    "try",
    "typedef",
    "undef",
    "union",
    "unless",
    "unsigned",
    "until",
    "use",
    "var",
    "virtual",
    "void",
    "volatile",
    "when",
    "while",
    "with",
    "xor",
    "yield",
    // Built-in types are also keywords.
    "binary",
    "bool",
    "byte",
    "double",
    "i16",
    "i32",
    "i64",
    "list",
    "map",
    "set",
    "string"
  )

  def contains(str: String): Boolean = set.contains(str)
}
