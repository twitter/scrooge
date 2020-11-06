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

object Main {
  def main(args: Array[String]): Unit = {
    val compiler = ScroogeOptionParser
      .parseOptions(args)
      .map(cfg => new Compiler(cfg))
      .getOrElse(
        throw new IllegalArgumentException(s"Failed to parse compiler args: ${args.mkString(",")}"))
    compiler.run()
  }
}
