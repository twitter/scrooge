package com.twitter.scrooge.adapt

import scala.collection.mutable

/**
 * Taken from scrooge/scrooge-generator/src/main/scala/com/twitter/scrooge/AST/Identifier.scala
 */
object CaseConverter {
  /**
   * convert string to camel case, with the following fine print:
   *   - leading underscores are preserved
   *   - internal underscores are removed. Character following an underscore
   *     is converted to upper case.
   *   - first character (non underscore char) is lower case
   *   - first character of the second and following parts (text between
   *     underscores) is always in upper case
   *   - if a part is all upper case it is converted to lower case (except for
   *     first character), in other cases case is preserved
   *
   *   Examples: (original, camel case)
   *     (gen_html_report, genHtmlReport)
   *     (GEN_HTML_REPORT, genHtmlReport)
   *     (Gen_HTMLReport, genHTMLReport)
   *     (Gen_HTML_Report, genHtmlReport)
   *     (GENHTMLREPORT, genhtmlreport)
   *     (genhtmlreport, genhtmlreport)
   *     (genHtmlReport, genHtmlReport)
   *     (genHTMLReport, genHTMLReport)
   *     (_genHtmlReport, _genHtmlReport)
   */
  def toCamelCase(str: String): String = {
    str.takeWhile(_ == '_') +
      str
        .split('_')
        .filterNot(_.isEmpty)
        .zipWithIndex
        .map { case (part, ind) =>
          val first = if (ind == 0) part(0).toLower else part(0).toUpper
          val isAllUpperCase = part.forall { c => c.isUpper || !c.isLetter }
          val rest = if (isAllUpperCase) part.drop(1).toLowerCase else part.drop(1)
          new mutable.StringBuilder(part.size).append(first).append(rest)
        }
        .mkString
  }
}

