/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scrooge.backend

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

object UpperCase {
  def apply(str: String) = str.toUpperCase
}