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

package com.twitter.scrooge.frontend

import java.io.File
import java.util.logging.Logger
import java.util.zip.{ZipFile, ZipEntry}
import scala.io.Source

// an imported file should have its own path available for nested importing.
case class FileContents(importer: Importer, data: String, thriftFilename: Option[String])

// an Importer turns a filename into its string contents.
trait Importer extends (String => Option[FileContents]) {
  private[scrooge] def canonicalPaths: Seq[String] // for tests
  def lastModified(filename: String): Option[Long]

  // Note: Deduplicates based on the equality of `Importer`s.
  def +:(head: Importer): Importer = head match {
    case MultiImporter(importers) => MultiImporter.deduped(head +: importers)
    case _ => MultiImporter.deduped(Seq(head, this))
  }

  /**
   * @param filename a filename to resolve.
   * @return the absolute path used to resolve it. Used to cache importer results.
   */
  private[scrooge] def getResolvedPath(filename: String): Option[String] = None
}

object Importer {
  val logger: Logger = Logger.getLogger(getClass.getName)

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
    MultiImporter.deduped(paths.map(Importer.apply))
}

final case class DirImporter(dir: File) extends Importer {
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
      Some((file, importer))
    } else None
  }

  def lastModified(filename: String): Option[Long] =
    resolve(filename).map { case (file, _) => file.lastModified }

  def apply(filename: String): Option[FileContents] =
    resolve(filename).map {
      case (file, importer) =>
        FileContents(importer, Source.fromFile(file, "UTF-8").mkString, Some(file.getName))
    }

  override private[scrooge] def getResolvedPath(filename: String): Option[String] = {
    resolve(filename).map { case (file, _) => file.getCanonicalPath }
  }

  override def toString: String = s"DirImporter(${dir.getCanonicalPath})"

  // We override equals and hashCode because
  // the default equality for File does not have the semantics we want.
  override def equals(other: Any): Boolean = {
    other match {
      case that: DirImporter =>
        this.dir.getCanonicalPath == that.dir.getCanonicalPath
      case _ => false
    }
  }

  override def hashCode: Int = this.dir.getCanonicalPath.hashCode
}

// jar files are just zip files, so this will work with both .jar and .zip files
final case class ZipImporter(file: File) extends Importer {
  private[this] val zipFile = new ZipFile(file)

  lazy val canonicalPaths: Seq[String] = Seq(file.getCanonicalPath)

  private def resolve(filename: String): Option[ZipEntry] =
    Option(zipFile.getEntry(filename))

  // uses the lastModified time of the zip/jar file
  def lastModified(filename: String): Option[Long] =
    resolve(filename).map { _ => file.lastModified }

  def apply(filename: String): Option[FileContents] =
    resolve(filename).map { entry =>
      FileContents(
        this,
        Source.fromInputStream(zipFile.getInputStream(entry), "UTF-8").mkString,
        Some(entry.getName)
      )
    }

  private[this] def canResolve(filename: String): Boolean = resolve(filename).isDefined

  override private[scrooge] def getResolvedPath(filename: String): Option[String] = {
    if (canResolve(filename))
      Some(new File(file, filename).getCanonicalPath)
    else
      None
  }

  override def toString: String = s"ZipImporter(${file.getCanonicalPath})"

  // We override equals and hashCode because
  // the default equality for File does not have the semantics we want.
  override def equals(other: Any): Boolean = {
    other match {
      case that: ZipImporter =>
        this.file.getCanonicalPath == that.file.getCanonicalPath
      case _ => false
    }
  }

  override def hashCode: Int = this.file.getCanonicalPath.hashCode

}

case class MultiImporter(importers: Seq[Importer]) extends Importer {
  lazy val canonicalPaths: Seq[String] = importers.flatMap { _.canonicalPaths }

  private def first[A](filename: String, f: Importer => Option[A]): Option[A] =
    importers.flatMap { importer => f(importer).map((_, importer)) } match {
      case Seq((resolved, _)) =>
        Some(resolved)
      case Seq() =>
        None
      case more =>
        val count = more.size
        val importers = more.map { case (_, importer) => importer.toString }.mkString(", ")
        Importer.logger.warning(
          s"Expected to find just 1 match for $filename, but found $count, in $importers"
        )
        Some(more.head._1)
    }

  def lastModified(filename: String): Option[Long] =
    first[Long](filename, { _.lastModified(filename) })

  def apply(filename: String): Option[FileContents] =
    first[FileContents](filename, { _(filename) }).map {
      case FileContents(importer, data, thriftFilename) =>
        // take the importer that found the file and prepend it to importer list returned,
        // that way it is used first when finding relative imports
        FileContents(importer +: this, data, thriftFilename)
    }

  override def +:(head: Importer): Importer = head match {
    case MultiImporter(others) => MultiImporter.deduped(others ++ importers)
    case _ => MultiImporter.deduped(head +: importers)
  }

  override private[scrooge] def getResolvedPath(filename: String): Option[String] =
    first[String](filename, { _.getResolvedPath(filename) })

  override def toString: String = s"MultiImporter(${importers.mkString(", ")})"
}

object MultiImporter {

  /**
   * Ensures that we don't add multiple importers that are pointing to the same directory to the
   * include path, while preserving importer order.
   */
  def deduped(importers: Seq[Importer]): MultiImporter = MultiImporter(importers.distinct)
}

object NullImporter extends Importer {
  val canonicalPaths: Nil.type = Nil
  def lastModified(filename: String): None.type = None
  def apply(filename: String): None.type = None
}
