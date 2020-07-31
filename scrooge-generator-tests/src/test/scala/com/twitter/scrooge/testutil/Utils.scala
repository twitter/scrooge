package com.twitter.scrooge.testutil

import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import java.io.{BufferedReader, InputStreamReader}
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
      | (\*)?   built at: .*""".stripMargin.r
  private[this] val headerNormalizedReplacement =
    """ $1   version: ?
      | $2   rev: ?
      | $3   built at: ?""".stripMargin

  def normalizeHeaders(generatedThrift: String): String = {
    // normalize the headers
    headerRegEx.replaceAllIn(generatedThrift, headerNormalizedReplacement)
  }
}
