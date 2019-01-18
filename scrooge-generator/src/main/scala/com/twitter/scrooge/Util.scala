package com.twitter.scrooge

/**
  * Utility methods for templating
  */
object Util {

  /** Get the Java version this is running in.
    *
    * Only supported ones currently are 8 and 11
    */
  lazy val javaVersion =
    Option(System.getProperty("java.version")) match {
      case Some(version) if version.startsWith("1.8") => Some(8)
      case Some(version) if version.startsWith("11.") => Some(11)
      case _ => None
    }
}
