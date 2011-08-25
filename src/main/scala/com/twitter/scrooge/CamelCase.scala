package com.twitter.scrooge

object CamelCase {
  private object State extends Enumeration {
    type State = Value
    val NextUp, NextDown, Lower, Upper = Value
  }

  def apply(str: String): String = apply(str, false)

  def apply(str: String, firstCharUp: Boolean): String = {
    import State._
    var state = if (firstCharUp) NextUp else NextDown
    val sb = new StringBuilder(str.length)

    // c should be upper only if following _ or following <lower> and is <upper>
    for (c <- str) {
      if (c == '_') {
        state = NextUp
      } else {
        state match {
          case NextUp => sb.append(c.toUpper)
          case NextDown => sb.append(c.toLower)
          case Lower => sb.append(c)
          case Upper => sb.append(c.toLower)
        }
        state = if (c.isUpper) Upper else Lower
      }
    }
    sb.toString
  }
}

object TitleCase {
  def apply(str: String) = CamelCase(str, true)
}
