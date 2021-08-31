package com.twitter.scrooge.ast

sealed abstract class Header extends HeaderNode

/**
 * Process include statement.
 * @param filePath the path of the file to be included. It can be a
 *                 relative path, an absolute path or simply a file name
 * @param document the content of the file to be included.
 */
case class Include(filePath: String, document: Document) extends Header {

  /**
   * The definitions in the included file must be used with a prefix.
   * For example, if it says
   *    include "../relativeDir/foo.thrift"
   * and include2.thrift contains a definition
   *    struct Bar {
   *      ..
   *    }
   * Then we can use type Bar like this:
   *    foo.Bar
   */
  val prefix: Identifier = Identifier(filePath.split('/').last.split('.').toSeq match {
    case Seq(v) => v
    case head :+ _ => head.mkString(".")
  })
}

case class CppInclude(file: String) extends Header

case class Namespace(language: String, id: Identifier) extends Header

/**
 * The path that returns a custom `com.twitter.scrooge.ThriftValidator`.
 *
 * @param path the fully qualified name of the custom `ThriftValidator`.
 *
 * @note if the validator can not be created from [[path]], a compile
 *       time error will be thrown.
 */
case class Validator(path: Identifier) extends Header
