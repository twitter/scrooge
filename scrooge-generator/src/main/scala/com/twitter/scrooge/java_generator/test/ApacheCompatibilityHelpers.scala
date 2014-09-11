package com.twitter.scrooge.java_generator.test

object ApacheCompatibilityHelpers {
  def cleanWhitespace(actual: String, cleanEmptySemicolons: Boolean) = {
    val cleanTypedefMetadata = """(new FieldValueMetaData\([A-Z,a-z,0-9.]*).*?\)\)\);""".r
    val randomWhitespace = """[ ]*\{""".r

    val values = actual.split("\n") map { s: String =>
      s.trim
    } filter { s =>
      !s.isEmpty
    } filter { s =>
      !s.startsWith("/**") && !s.startsWith("*")
    } filter { s =>
      !cleanEmptySemicolons || !s.equals(";")
    } map { s =>
      val clean1 = cleanTypedefMetadata.findFirstMatchIn(s) match {
        case Some(m) => m.group(1) + ")));"
        case None => s
      }
      randomWhitespace.replaceAllIn(clean1, " {")
    }
    values
  }

}
