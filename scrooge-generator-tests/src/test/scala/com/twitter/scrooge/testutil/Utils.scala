package com.twitter.scrooge.testutil

import com.twitter.io.StreamIO
import org.scalatest.matchers.must.Matchers
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Arrays
import java.util.Collections
import java.io.ByteArrayOutputStream

object Utils extends Matchers {
  def verify(
    actual: String,
    expected: String,
    assertionMessage: String = "The contents did not match"
  ): Unit = {
    val actualItems = normalizeHeaders(actual).split("\n")
    val expectedItems = normalizeHeaders(expected).split("\n")
    for (i <- 0 until actualItems.size) {
      if (!actualItems(i).equals(expectedItems(i))) {
        println("Actual: " + actualItems(i))
        println("Expected: " + expectedItems(i))
        // No idea why the one below doesn't make the test red
        assert(actualItems(i) == expectedItems(i), assertionMessage)
      } else {
        // println(expectedItems(i))
      }
      actualItems(i) must be(expectedItems(i))
    }
  }

  def verifyWithHint(
    actual: String,
    expectedResourcePath: String,
    thriftSourcePath: String,
    generatedFilePath: String,
    language: String
  ): Unit = {
    val hint = "To regenerate the file run scrooge with these options:\n" +
      s"--language $language --finagle --gen-adapt --dest /tmp/struct " +
      s"$$SCROOGE_ROOT/scrooge-generator-tests/src/test/resources/$thriftSourcePath\n" +
      s"Then move the generated file to the correct place\n" +
      s"mv /tmp/struct/$generatedFilePath $$SCROOGE_ROOT/scrooge-generator-tests/src/test/resources/$expectedResourcePath"
    verify(actual, getFileContents(expectedResourcePath), hint)
  }

  def getFileContents(resource: String): String = {
    val ccl = Thread.currentThread().getContextClassLoader
    val is = ccl.getResourceAsStream(resource)
    try streamToString(is)
    finally is.close()
  }

  def streamToString(is: InputStream): String = {
    new String(streamToBytes(is), StandardCharsets.UTF_8)
  }

  def streamToBytes(is: InputStream): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    StreamIO.copy(is, out)
    out.toByteArray()
  }

  private[this] val headerRegEx =
    """ (\*)?   version: .*
      | (\*)?   rev: .*
      | (\*)?   built at: .*
      | (\*)?   source file: .*""".stripMargin.r
  private[this] val headerNormalizedReplacement =
    """ $1   version: ?
      | $2   rev: ?
      | $3   built at: ?
      | $4   source file: ?""".stripMargin

  def normalizeHeaders(generatedThrift: String): String = {
    // normalize the headers
    headerRegEx.replaceAllIn(generatedThrift, headerNormalizedReplacement)
  }

  /**
   * This method unpacks and extracts resource from a JAR file. This was added to support
   * bazel while also supporting pants and sbt as the scrooge-generator-tests loads resource file
   * from the file system.
   * @param file file name for the resource file
   * @param directory directory to the file that is about to be used as a resource file
   */
  def getIncludeFilePath(file: String, directory: Option[String]): String = {
    val ccl = Thread.currentThread().getContextClassLoader
    // get the path from the given directory
    // file would be the file name and dir is the directory excluding file name: goldfiles/some.thrift
    val path = directory match {
      case Some(dir) => s"$dir/$file"
      case _ => file
    }
    // we get the resource file from the path
    val inputThrift = ccl.getResource(path).toURI
    val protocol = inputThrift.getScheme.toLowerCase

    // if resource URI scheme is `jar` this condition is for bazel since it loads resource as a jar
    if (protocol == "jar") {
      // In order to handle Thrift files that import other files, we need to extract the whole directory
      val fs =
        try {
          FileSystems.getFileSystem(inputThrift)
        } catch {
          case _: FileSystemNotFoundException =>
            FileSystems.newFileSystem(inputThrift, Collections.emptyMap[String, String])
        }

      Files
        .walk(fs.getPath(path).getParent, FileVisitOption.FOLLOW_LINKS)
        .filter(a => Files.isRegularFile(a))
        .forEach { a =>
          // Convert the path to a string before resolving because it belongs to a different filesystem than thriftInputRoots
          val out = thriftInputsRoot.resolve(a.toString)
          // We're likely to visit the same jar file multiple times for different Thrift inputs, so skip files that already exist
          if (!Files.exists(out)) {
            Files.createDirectories(out.getParent)
            Files.copy(a, out)
          }
        }
      thriftInputsRoot.resolve(path).toString
    } else if (protocol == "file") {
      inputThrift.getPath // this condition is for pants and sbt as they load resources from a file path
    } else {
      throw new RuntimeException(s"Unsupported protocol: $protocol")
    }
  }

  /**
   * Temporary directory that Thrift inputs from JAR files on the classpath are extracted into
   */
  private[this] lazy val thriftInputsRoot: Path = {
    val root = Files.createTempDirectory("scrooge-thrift-inputs")
    sys.addShutdownHook {
      Files.walkFileTree(
        root,
        new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            // If deleting a file in the directory failed, `exc` will ne non-null
            if (exc == null) {
              Files.delete(dir)
              FileVisitResult.CONTINUE
            } else throw exc
          }
        }
      )
    }
    root
  }
}
