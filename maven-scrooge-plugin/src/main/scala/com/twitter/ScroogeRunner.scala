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

package com.twitter

import org.apache.maven.plugin.logging.Log
import java.io.File
import java.util.{Map, Set}
import scala.collection.JavaConverters._
import scrooge.Compiler

class ScroogeRunner {

  def compile(
    log: Log,
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    flags: Set[String]
  ) {

    val compiler = new Compiler()
    compiler.strict = true
    compiler.destFolder = outputDir.getPath
    compiler.verbose = true
    thriftFiles.asScala.map { compiler.thriftFiles += _.getPath }
    thriftIncludes.asScala.map { compiler.importPaths += _.getPath }
    namespaceMappings.asScala.map { e => compiler.namespaceMappings.put(e._1, e._2)}
    compiler.run()
  }
}