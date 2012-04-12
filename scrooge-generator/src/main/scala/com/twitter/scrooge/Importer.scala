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

import java.io.{IOException, File}
import scala.collection.Map
import scala.io.Source

// an Importer turns a filename into its string contents.
trait Importer extends (String => String) {
  def lastModified(filename: String): Option[Long]
}

object Importer {
  def fileImporter(importPaths: Seq[String]) = new Importer {
    val paths = List(".") ++ importPaths

    private def resolve(filename: String): File = {
      val f = new File(filename)
      if (f.isAbsolute) {
        f
      } else {
        paths.map { path => new File(path, filename) }.find { _.canRead } getOrElse {
          throw new IOException("Can't find file: " + filename)
        }
      }
    }

    def lastModified(filename: String): Option[Long] = {
      Some(resolve(filename).lastModified)
    }

    // find the requested file, and load it into a string.
    def apply(filename: String): String = {
      Source.fromFile(resolve(filename)).mkString
    }
  }

  def fakeImporter(files: Map[String, String]) = new Importer {
    def lastModified(filename: String) = None
    def apply(filename: String): String = {
      files.get(filename).getOrElse {
        throw new IOException("Can't find file: " + filename)
      }
    }
  }

  def resourceImporter(c: Class[_]) = new Importer {
    def lastModified(filename: String) = None
    def apply(filename: String): String = {
      try {
        Source.fromInputStream(c.getResourceAsStream(filename)).mkString
      } catch {
        case e =>
          throw new IOException("Can't load resource: " + filename)
      }
    }
  }
}
