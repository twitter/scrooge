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

package com.twitter.scrooge.frontend

import java.io.File
import java.util.zip.{ZipFile, ZipEntry}
import scala.io.Source

// an imported file should have its own path available for nested importing.
case class FileContents(importer: Importer, data: String, thriftFilename: Option[String])

// an Importer turns a filename into its string contents.
trait Importer extends (String => Option[FileContents]) {
  private[scrooge] def canonicalPaths: Seq[String] // for tests
  def lastModified(filename: String): Option[Long]
  def +:(head: Importer): Importer = MultiImporter(Seq(head, this))
  /**
   * @param filename a filename to resolve.
   * @return the absolute path used to resolve it. Used to cache importer results.
   */
  private[scrooge] def getResolvedPath(filename: String): Option[String] = None
}

object Importer {
  /**
   * @param path Path of a directory or a zip/jar file
   * @return the importer that will look into the directory or zip/jar
   *         file specified by path
   */
  def apply(path: String): Importer =
    apply(new File(path).getCanonicalFile)

  /**
   * @param file File of a directory or a zip/jar file
   * @return the importer that will look into the directory or zip/jar
   *         file specified by path
   */
  def apply(file: File): Importer = {
    if (file.isDirectory)
      DirImporter(file)
    else if (file.isFile &&
      (file.getName.toLowerCase.endsWith(".jar") ||
        file.getName.toLowerCase.endsWith(".zip")))
      // A more accurate way to determine whether it's a zip file is to call
      //     new ZipFile(file)
      // and see if there is an exception. But we decide to use the filename
      // because more often than not it indicates user's intention.
      ZipImporter(file)
    else
      NullImporter
  }

  def apply(paths: Seq[String]): Importer =
    MultiImporter(paths map { Importer(_) })
}

case class DirImporter(dir: File) extends Importer {
  private[scrooge] def canonicalPaths = Seq(dir.getCanonicalPath) // for test

  private[this] def resolve(filename: String): Option[(File, Importer)] = {
    val file = {
      val file = new File(filename)
      if (file.isAbsolute)
        file
      else
        new File(dir, filename)
    }
    if (file.canRead) {
      // if the file does not exactly locate in dir, we need to add a new relative path
      val importer =
        if (file.getParentFile.getCanonicalPath == dir.getCanonicalPath)
          this
        else
          Importer(file.getParentFile) +: this
      Some(file, importer)
    } else None
  }

  def lastModified(filename: String): Option[Long] =
    resolve(filename) map { case (file, _) => file.lastModified }

  def apply(filename: String): Option[FileContents] =
    resolve(filename) map { case (file, importer) =>
      FileContents(importer, Source.fromFile(file, "UTF-8").mkString, Some(file.getName))
    }

  override private[scrooge] def getResolvedPath(filename: String): Option[String] = {
    resolve(filename).map { case (file, _) => file.getCanonicalPath }
  }
}

// jar files are just zip files, so this will work with both .jar and .zip files
case class ZipImporter(file: File) extends Importer {
  private[this] val zipFile = new ZipFile(file)

  lazy val canonicalPaths = Seq(file.getCanonicalPath)

  private def resolve(filename: String): Option[ZipEntry] =
    Option(zipFile.getEntry(filename))

  // uses the lastModified time of the zip/jar file
  def lastModified(filename: String): Option[Long] =
    resolve(filename) map { _ => file.lastModified }

  def apply(filename: String): Option[FileContents] =
    resolve(filename) map { entry =>
      FileContents(this, Source.fromInputStream(zipFile.getInputStream(entry), "UTF-8").mkString, Some(entry.getName))
    }

  private[this] def canResolve(filename: String): Boolean = resolve(filename).isDefined

  override private[scrooge] def getResolvedPath(filename: String): Option[String] = {
    if (canResolve(filename))
      Some(new File(file, filename).getCanonicalPath)
    else
      None
  }
}

case class MultiImporter(importers: Seq[Importer]) extends Importer {
  lazy val canonicalPaths = importers flatMap { _.canonicalPaths }

  private def first[A](f: Importer => Option[A]): Option[A] =
    importers.foldLeft[Option[A]](None) { (accum, next) => accum orElse f(next) }

  def lastModified(filename: String): Option[Long] =
    first[Long] { _.lastModified(filename) }

  def apply(filename: String): Option[FileContents] =
    first[FileContents] { _(filename) } map {
      case FileContents(importer, data, thriftFilename) =>
        // take the importer that found the file and prepend it to importer list returned,
        // that way it is used first when finding relative imports
        FileContents(importer +: this, data, thriftFilename)
    }

  override def +:(head: Importer): Importer = MultiImporter(head +: importers)

  override private[scrooge] def getResolvedPath(filename: String): Option[String] =
    first[String] { _.getResolvedPath(filename) }
}

object NullImporter extends Importer {
  val canonicalPaths = Nil
  def lastModified(filename: String) = None
  def apply(filename: String) = None
}
