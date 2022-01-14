package com.twitter.scrooge

/**
 * Collection of Java keywords so they can be filtered out of java-generated code.
 *
 * @note many of these keywords are also they're included in [[ThriftKeywords]]
 *       Though they should not appear as fieldnames in any thrift files, there
 *       are some exceptions, such as when these are used as, say, namespaces.
 *       For those reasons, we leave the redunant keywords here.
 */
private object JavaKeywords {
  private[this] val set = Set[String](
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
  )

  def contains(str: String): Boolean = set.contains(str)
}
