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

package com.twitter.scrooge

import com.twitter.conversions.string._
import com.twitter.handlebar.Handlebar
import scala.collection.mutable.HashMap
import scala.io.Source
import scala.util.parsing.input.StreamReader
import java.io.{FileInputStream, InputStreamReader}

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
          new Handlebar(StreamReader(new InputStreamReader(inputStream)))
        }
      }
    )
  }
}
