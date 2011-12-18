package com.twitter.scrooge

import com.twitter.conversions.string._
import com.twitter.handlebar.Handlebar
import scala.collection.mutable.HashMap
import scala.io.Source

class HandlebarLoader(prefix: String, suffix: String = ".scala") {
  private val cache = new HashMap[String, Handlebar]

  def apply(name: String): Handlebar = {
    val fullName = prefix + name + suffix
    cache.getOrElseUpdate(name,
      getClass.getResourceAsStream(fullName) match {
        case null => {
          throw new NoSuchElementException("template not found: " + fullName)
        }
        case inputStream => {
          new Handlebar(Source.fromInputStream(inputStream).getLines().mkString("\n"))
        }
      }
    )
  }
}
