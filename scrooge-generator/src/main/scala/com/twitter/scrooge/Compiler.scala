/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scrooge

import com.twitter.scrooge.ast.Document
import com.twitter.scrooge.backend.{GeneratorFactory, ScalaGenerator}
import com.twitter.scrooge.frontend.{FileParseException, Importer, NullImporter, ThriftParser, TypeResolver}
import com.twitter.scrooge.java_generator.ApacheJavaGenerator

import java.io.{File, FileWriter}
import scala.collection.concurrent.TrieMap

object CompilerDefaults {
  val language: String = "scala"
  val defaultNamespace: String = "thrift"
}

class Compiler(val config: ScroogeConfig) {
  var fileMapWriter: scala.Option[FileWriter] = None

  def run(): Unit = {
    // if --gen-file-map is specified, prepare the map file.
    fileMapWriter = config.fileMapPath.map { path =>
      val file = new File(path)
      val dir = file.getParentFile
      if (dir != null && !dir.exists()) {
        dir.mkdirs()
      }
      if (config.verbose) {
        println("+ Writing file mapping to %s".format(path))
      }
      new FileWriter(file)
    }

    val importer = (if(config.addRootDirImporter) Importer(new File(".")) else NullImporter) +: Importer(config.includePaths.toSeq)

    val isJava = config.language.equals("java")
    val documentCache = new TrieMap[String, Document]

    // compile
    for (inputFile <- config.thriftFiles) {
      try {
        val parser = new ThriftParser(
          importer,
          config.strict,
          defaultOptional = isJava,
          skipIncludes = false,
          documentCache
        )
        val doc = parser.parseFile(inputFile).mapNamespaces(config.namespaceMappings)

        if (config.verbose) println("+ Compiling %s".format(inputFile))
        val resolvedDoc = TypeResolver()(doc)
        val generator =
          GeneratorFactory(
            config.language,
            resolvedDoc,
            config.defaultNamespace,
            config.languageFlags)

        generator match {
          case g: ScalaGenerator => g.warnOnJavaNamespaceFallback = config.scalaWarnOnJavaNSFallback
          case g: ApacheJavaGenerator => g.serEnumType = config.javaSerEnumType
          case _ => ()
        }

        val generatedFiles = generator(
          config.flags,
          new File(config.destFolder),
          config.dryRun,
          config.genAdapt
        ).map {
          _.getPath
        }
        if (config.verbose) {
          println("+ Generated %s".format(generatedFiles.mkString(", ")))
        }
        fileMapWriter.foreach { w =>
          generatedFiles.foreach { path => w.write(inputFile + " -> " + path + "\n") }
        }
      } catch {
        case e: Throwable => throw new FileParseException(inputFile, e)
      }
    }

    // flush and close the map file
    fileMapWriter.foreach { _.close() }
  }
}
