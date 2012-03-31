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

import java.io.File
import com.twitter.scrooge.AST.{Service, Document}

abstract sealed class ServiceOption

case object WithFinagleClient extends ServiceOption
case object WithFinagleService extends ServiceOption
case object WithOstrichServer extends ServiceOption
case class JavaService(service: Service, options: Set[ServiceOption])

/**
 * Useful common code for generating templated code out of the thrift AST.
 */
trait Generator {
  class InternalError(description: String) extends Exception(description)

  def outputFile(destFolder: String, doc0: Document, inputFile: String): File
  def apply(_doc: Document, serviceOptions: Set[ServiceOption]): String

  implicit def string2indent(underlying: String) = new Object {
    def indent(level: Int = 1): String = underlying.split("\\n").map { ("  " * level) + _ }.mkString("\n")
    def indent: String = indent(1)
  }

  implicit def seq2indent(underlying: Seq[String]) = new Object {
    def indent(level: Int = 1): String = underlying.mkString("\n").indent(level)
    def indent: String = indent(1)
  }

  implicit def array2indent(underlying: Array[String]) = new Object {
    def indent(level: Int = 1): String = underlying.mkString("\n").indent(level)
    def indent: String = indent(1)
  }
}
