package com.twitter.scrooge.testutil

import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.twitter.io.ClasspathResource
import com.twitter.io.StreamIO
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import org.scalatest.matchers.must.Matchers

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
    val br = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8))
    try CharStreams.toString(br)
    finally br.close()
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
    val inputThrift = ccl.getResource(path).getPath

    // if the the file contains a jar this condition is for bazel since it loads resource as a jar
    if (inputThrift.contains(".jar!")) {
      // load the named resource from the classpath
      // opened resource returns input stream
      ClasspathResource.load(path) match {
        case Some(inputStream) =>
          val tempFolder = System.getProperty("java.io.tmpdir")
          // creates a temporary thread-local folder for a block of code to execute in.
          // The folder is recursively deleted after the test.
          var folder: File = new File(tempFolder, "scrooge-test-" + System.currentTimeMillis)
          while (!folder.mkdir()) {
            folder = new File(tempFolder, "scrooge-test-" + System.currentTimeMillis)
          }

          folder.deleteOnExit() // delete folder on exit

          val tempPath = directory match {
            case Some(dir) => s"${folder.getCanonicalPath}/$dir"
            case _ => folder.getCanonicalPath
          }
          val out = File.createTempFile(tempPath, file)
          out.deleteOnExit()
          // buffer the given input stream
          val byteArrayOutputStream = StreamIO.buffer(inputStream)
          val outputStream = new FileOutputStream(out, true)
          // Writes the complete contents of this byte array output stream to the specified output stream argument,
          byteArrayOutputStream.writeTo(outputStream)
          out.getCanonicalPath

        case _ => throw new IllegalArgumentException(s"couldn't find $path as a resource")
      }
    } else
      inputThrift // this condition is for pants and sbt as they load resources from a file path

  }
}
