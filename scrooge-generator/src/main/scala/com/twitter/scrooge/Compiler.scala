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

import scala.collection.mutable
import java.io.{File, FileWriter}
import com.twitter.scrooge.backend.{Generator, ScalaGenerator, ServiceOption}
import com.twitter.scrooge.frontend.{TypeResolver, ThriftParser, Importer}
import org.apache.commons.lang.time.FastDateFormat
import java.util.Date

class Compiler {
  val defaultDestFolder = "."
  var destFolder: String = defaultDestFolder
  val includePaths = new mutable.ListBuffer[String]
  val thriftFiles = new mutable.ListBuffer[String]
  val flags = new mutable.HashSet[ServiceOption]
  val namespaceMappings = new mutable.HashMap[String, String]
  var verbose = false
  var strict = true
  var skipUnchanged = false
  var experimentFlags = new mutable.ListBuffer[String]
  var fileMapPath: Option[String] = None
  var fileMapWriter: Option[FileWriter] = None
  var dryRun: Boolean = false
  var language: String = "scala"
  var defaultNamespace: String = "thrift"
  var scalaWarnOnJavaNSFallback: Boolean = false
  val now: String = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date())

  def run() {
    // if --gen-file-map is specified, prepare the map file.
    fileMapWriter = fileMapPath.map { path =>
      val file = new File(path)
      val dir = file.getParentFile
      if (dir != null && !dir.exists()) {
        dir.mkdirs()
      }
      if (verbose) {
        println("+ Writing file mapping to %s".format(path))
      }
      new FileWriter(file)
    }

    val importer = Importer(new File(".")) +: Importer(includePaths)

    // compile
    for (inputFile <- thriftFiles) {
      val isJava = language.equals("java")
      val parser = new ThriftParser(importer, strict, defaultOptional = isJava)
      val doc0 = parser.parseFile(inputFile).mapNamespaces(namespaceMappings.toMap)

      if (verbose) println("+ Compiling %s".format(inputFile))
      val resolvedDoc = TypeResolver()(doc0) // TODO: THRIFT-54
      val generator = Generator(
        language,
        resolvedDoc.resolver.includeMap,
        defaultNamespace,
        now,
        experimentFlags)

      generator match {
        case g: ScalaGenerator => g.warnOnJavaNamespaceFallback = scalaWarnOnJavaNSFallback
        case _ => ()
      }

      val generatedFiles = generator(
        resolvedDoc.document,
        flags.toSet,
        new File(destFolder),
        dryRun
      ).map { _.getPath }

      if (verbose) {
        println("+ Generated %s".format(generatedFiles.mkString(", ")))
      }
      fileMapWriter.foreach { w =>
        generatedFiles.foreach { path =>
          w.write(inputFile + " -> " + path + "\n")
        }
      }
    }

    // flush and close the map file
    fileMapWriter.foreach { _.close() }
  }
}
