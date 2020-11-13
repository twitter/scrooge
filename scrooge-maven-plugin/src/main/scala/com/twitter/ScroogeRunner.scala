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

package com.twitter

import com.twitter.scrooge.{Compiler, ScroogeConfig, ScroogeOptionParser}
import java.io.File
import java.util.{Map, Set}
import scala.collection.JavaConverters._

class ScroogeRunner {

  def compile(
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    language: String,
    flags: Set[String]
  ): Unit = {
    val args = flags.asScala.toSeq ++ thriftFiles.asScala.map { _.getPath }
    val optConfig: Option[ScroogeConfig] = ScroogeOptionParser.parseOptions(
      args,
      defaultConfig = ScroogeConfig(
        destFolder = outputDir.getPath,
        includePaths = thriftIncludes.asScala.map { _.getPath }.toList,
        namespaceMappings = namespaceMappings.asScala.toMap,
        language = language.toLowerCase
      )
    )

    optConfig match {
      case Some(cfg) =>
        val compiler = new Compiler(cfg)
        compiler.run()
      case _ =>
        throw new IllegalArgumentException(s"Failed to parse compiler args: ${args.mkString(",")}")
    }
  }
}
