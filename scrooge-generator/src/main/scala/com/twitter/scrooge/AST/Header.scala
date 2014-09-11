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
  val prefix: SimpleID = SimpleID(filePath.split('/').last.split('.').head)
}

case class CppInclude(file: String) extends Header

case class Namespace(language: String, id: Identifier) extends Header
