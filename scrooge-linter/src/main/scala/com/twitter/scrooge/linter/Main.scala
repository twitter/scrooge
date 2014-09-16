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

package com.twitter.scrooge.linter

import java.io.File
import java.util.Properties
import scopt.OptionParser

case class Config(
  val verbose: Boolean = false,
  val strict: Boolean = false,
  val files: Seq[String] = Seq(),
  val ignoreErrors: Boolean = false
)


object Main {
  def main(args: Array[String]) {
    new Linter(parseOptions(args)).lint()
  }

  def parseOptions(args: Seq[String]): Config = {
    val buildProperties = new Properties
    Option(getClass.getResource("build.properties")) foreach { resource =>
      buildProperties.load(resource.openStream)
    }

    val parser = new OptionParser[Config]("scrooge-linter") {
        help("help") text ("show this help screen")

        opt[Unit]('V', "version") text ("print version and quit") action { (_, c) => {
          println("scrooge linter " + buildProperties.getProperty("version", "0.0"))
          println("    build " + buildProperties.getProperty("build_name", "unknown"))
          println("    git revision " + buildProperties.getProperty("build_revision", "unknown"))
          sys.exit()
          c
        }}

        opt[Boolean]('v', "verbose") text ("log verbose messages about progress") action { (_, c) => {
          c.copy(verbose = true)
          c
        }}

        opt[Boolean]('i', "ignore-errors") text ("continue if linter errors are found (for batch processing)") action { (_, c) => {
          c.copy(ignoreErrors = true)
          c
        }}

        opt[Boolean]("disable-strict") text ("issue warnings on non-severe parse errors instead of aborting") action { (_, c) =>
          c.copy(strict = false)
          c
        }

        opt[String]("<files...>") text ("thrift files to compile") action { (input, c) =>
          c.copy(files = c.files :+ input)
        }
    }

    parser.parse(args, Config()) match {
      case Some(cfg) => cfg
      case None =>
        sys.exit()
    }
  }
}
