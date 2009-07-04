package com.twitter.scrooge

import java.io.{BufferedReader, InputStream, InputStreamReader, IOException, File, FileInputStream}
import scala.collection.mutable
import parser.ParseException

class Importer(importPaths: String*) {
  val paths = List(".") ++ importPaths

  // find the requested file, and load it into a string.
  def apply(filename: String): String = {
    val f = new File(filename)
    val file = if (f.isAbsolute) {
      f
    } else {
      paths.projection.map { path => new File(path, filename) }.find { _.canRead } getOrElse {
        throw new IOException("Can't find file: " + filename)
      }
    }

    try {
      streamToString(new FileInputStream(file))
    } catch {
      case x => throw new ParseException(x.toString)
    }
  }

  private val BUFFER_SIZE = 8192

  protected def streamToString(in: InputStream): String = {
    val reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))
    val buffer = new Array[Char](BUFFER_SIZE)
    val out = new StringBuilder
    var n = 0
    while (n >= 0) {
      n = reader.read(buffer, 0, buffer.length)
      if (n >= 0) {
        out.append(buffer, 0, n)
      }
    }
    out.toString
  }
}
