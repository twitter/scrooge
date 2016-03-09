package com.twitter.scrooge.goldfile

import com.twitter.io.Files
import com.twitter.scrooge.Main
import com.twitter.scrooge.testutil.TempDirectory
import java.io.File
import java.util.regex.Pattern
import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.io.Source

/**
 * Compares the output of a generator to a set of "golden files".
 *
 * This helps to viscerally see how changes to the generator affect the
 * generated code at code review time. Most updates to the gold files are
 * intentional due to the corresponding changes in the generator code or
 * templates.
 */
abstract class GoldFileTest extends FunSuite
  with BeforeAndAfter {

  private var tempDir: File = _

  before {
    tempDir = TempDirectory.create(None, deleteAtExit = false)
  }

  after {
    if (!Files.delete(tempDir))
      fail(s"Failed to delete $tempDir")
  }

  protected def language: String

  private val version = " \\*   version: .*".r
  private val rev =     " \\*   rev: .*".r
  private val builtAt = " \\*   built at: .*".r
  private val trailingSpaces = Pattern.compile("[ ]+$", Pattern.MULTILINE)

  private def generatedFiles(f: File): Seq[File] = {
    def accumulate(f: File, buf: Vector[File]): Vector[File] = {
      if (f.isFile) {
        buf :+ f
      } else {
        var bb = buf
        f.listFiles.foreach { f2 =>
          bb = accumulate(f2, bb)
        }
        bb
      }
    }
    accumulate(f, Vector.empty)
  }

  test("generated output looks as expected") {
    val ccl = Thread.currentThread().getContextClassLoader
    val inputThrift = ccl.getResource("gold_file_input/gold.thrift").getPath

    Main.main(Array(
      "--language", language,
      "--finagle",
      "--dest", tempDir.getPath,
      inputThrift))

    def generatedDataFor(file: File): String = {
      val gen = Source.fromFile(file, "UTF-8").mkString

      // normalize the headers
      val headersNormalized = builtAt.replaceFirstIn(
        rev.replaceFirstIn(
          version.replaceFirstIn(gen,
            " *   version: ?"),
          " *   rev: ?"),
        " *   built at: ?")

      trailingSpaces.matcher(headersNormalized).replaceAll("")
    }

    def goldDataFor(suffix: String): String = {
      val is = ccl.getResourceAsStream(s"gold_file_output_$language/$suffix")
      if (is == null)
        return ""
      Source.fromInputStream(is, "UTF-8").mkString
    }

    val gens = generatedFiles(tempDir)
    gens.foreach { gen =>
      // we want to take the path after tempDir and compare to suffixed gold file
      // in our resources dir.
      // the +1 removes what would be a leading slash from suffix
      val suffix = gen.toString.drop(tempDir.toString.length + 1)

      val genStr = generatedDataFor(gen)
      val expected = goldDataFor(suffix)
      withClue(suffix) {
        if (genStr != expected) {
          val msg =
            s"""
               |The generated file ${gen.getName} did not match gold file
               |"scrooge/scrooge-generator-tests/src/test/resources/gold_file_output_$language/$suffix".
               |Compare the output in stdout to the gold file and
               |either fix the generator or update the gold file to match.
             """.stripMargin
          println(msg)
          println(s"Generated file $suffix:\n${genStr}<<<EOF")
          fail(msg)
        }
      }
    }
  }

}
