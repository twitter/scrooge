package com.twitter.scrooge

import com.twitter.conversions.string._
import org.monkey.mustache.Mustache
import scala.collection.mutable.HashMap
import scala.io.Source

class MustacheLoader(prefix: String, suffix: String = ".scala") {
  private val cache = new HashMap[String, Mustache]

  def apply(name: String): Mustache = {
    val fullName = prefix + name + suffix
    cache.getOrElseUpdate(name,
      getClass.getResourceAsStream(fullName) match {
        case null => {
          throw new NoSuchElementException("template not found: " + fullName)
        }
        case inputStream => {
          val data = Source.fromInputStream(inputStream).getLines().mkString("\n")
          val flattenedData = data.regexSub("""(?s)\n\s*(\{\{[\#^/!][^}]+\}\})\n(?!\})""".r) { m =>
            "\n" + m.group(1)
          }
          new Mustache(flattenedData)
        }
      }
    )
  }
}
