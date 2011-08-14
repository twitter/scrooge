package com.twitter.scrooge

import java.io.{IOException, File}
import scala.collection.Map
import scala.io.Source

// an Importer turns a filename into its string contents.
trait Importer extends (String => String)

object Importer {
  def fileImporter(importPaths: Seq[String]) = new Importer {
    val paths = List(".") ++ importPaths

    // find the requested file, and load it into a string.
    def apply(filename: String): String = {
      val f = new File(filename)
      val file = if (f.isAbsolute) {
        f
      } else {
        paths.map { path => new File(path, filename) }.find { _.canRead } getOrElse {
          throw new IOException("Can't find file: " + filename)
        }
      }

      Source.fromFile(file).mkString
    }
  }

  def fakeImporter(files: Map[String, String]) = new Importer {
    def apply(filename: String): String = {
      files.get(filename).getOrElse {
        throw new IOException("Can't find file: " + filename)
      }
    }
  }

  def resourceImporter(c: Class[_]) = new Importer {
    def apply(filename: String): String = {
      try {
        Source.fromInputStream(c.getResourceAsStream(filename)).mkString
      } catch {
        case e =>
          throw new IOException("Can't load resource: " + filename)
      }
    }
  }
}
