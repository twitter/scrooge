package com.twitter.scrooge

import java.io.{IOException, File}
import scala.collection.Map
import scala.io.Source

// an Importer turns a filename into its string contents.
trait Importer extends (String => String) {
  def lastModified(filename: String): Option[Long]
}

object Importer {
  def fileImporter(importPaths: Seq[String]) = new Importer {
    val paths = List(".") ++ importPaths

    private def resolve(filename: String): File = {
      val f = new File(filename)
      if (f.isAbsolute) {
        f
      } else {
        paths.map { path => new File(path, filename) }.find { _.canRead } getOrElse {
          throw new IOException("Can't find file: " + filename)
        }
      }
    }

    def lastModified(filename: String): Option[Long] = {
      Some(resolve(filename).lastModified)
    }

    // find the requested file, and load it into a string.
    def apply(filename: String): String = {
      Source.fromFile(resolve(filename)).mkString
    }
  }

  def fakeImporter(files: Map[String, String]) = new Importer {
    def lastModified(filename: String) = None
    def apply(filename: String): String = {
      files.get(filename).getOrElse {
        throw new IOException("Can't find file: " + filename)
      }
    }
  }

  def resourceImporter(c: Class[_]) = new Importer {
    def lastModified(filename: String) = None
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
