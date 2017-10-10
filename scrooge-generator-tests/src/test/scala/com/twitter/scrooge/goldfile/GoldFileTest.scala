package com.twitter.scrooge.goldfile

import com.twitter.io.Files
import com.twitter.scrooge.Main
import com.twitter.scrooge.testutil.TempDirectory
import java.io.File
import java.util.regex.Pattern
import org.scalatest.{BeforeAndAfterAll, FunSuite}
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
  with BeforeAndAfterAll {

  private var tempDir: File = _
  protected var generatedFiles: Seq[File] = _

  override protected def beforeAll(): Unit = {
    tempDir = TempDirectory.create(None, deleteAtExit = false)
    if (!deleteTempFiles) {
      println(s"Temp dir $tempDir")
    }

    val ccl = Thread.currentThread().getContextClassLoader

    val inputThrifts = testThriftFiles.map { r =>
      Option(ccl.getResource(r))
        .getOrElse(sys.error(s"Couldn't find resource: $r"))
        .getPath
    }

    val args = Seq(
      "--language", language,
      "--finagle",
      "--gen-adapt",
      "--dest", tempDir.getPath) ++ inputThrifts

    Main.main(args.toArray)
    generatedFiles = generatedFiles(tempDir)
  }

  final def relativePath(f: File): String = {
    val root = tempDir.getAbsolutePath
    val b = f.getAbsolutePath
    assert(b.startsWith(root), s"Cannot relativize $b, not under root $root")
    b.substring(root.length+1, b.length)
  }

  override protected def afterAll(): Unit = {
    if (deleteTempFiles) {
      if (!Files.delete(tempDir))
        fail(s"Failed to delete $tempDir")
    }
  }

  protected def language: String
  protected def deleteTempFiles: Boolean = true

  private val headerRegEx =
    """ (\*)?   version: .*
      | (\*)?   rev: .*
      | (\*)?   built at: .*""".stripMargin.r
  private val headerNormalizedReplacement =
    """ $1   version: ?
      | $2   rev: ?
      | $3   built at: ?""".stripMargin
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

  protected def testThriftFiles = Seq("gold_file_input/gold.thrift")
  protected def goldFilesRoot: String = s"gold_file_output_$language"

  test("generated output looks as expected") {
    val ccl = Thread.currentThread().getContextClassLoader

    def generatedDataFor(file: File): String = {
      val gen = Source.fromFile(file, "UTF-8").mkString

      // normalize the headers
      val headersNormalized = headerRegEx.replaceAllIn(gen, headerNormalizedReplacement)

      trailingSpaces.matcher(headersNormalized).replaceAll("")
    }

    def goldDataFor(suffix: String): String = {
      val path = s"$goldFilesRoot/$suffix"
      val is = ccl.getResourceAsStream(path)
      if (is == null)
        return ""
      Source.fromInputStream(is, "UTF-8").mkString
    }

    generatedFiles.foreach { gen =>
      // we want to take the path after tempDir and compare to suffixed gold file
      // in our resources dir.
      // the +1 removes what would be a leading slash from suffix
      val suffix = gen.toString.drop(tempDir.toString.length + 1)

      val genStr = generatedDataFor(gen)
      val expected = goldDataFor(suffix)
      withClue(suffix) {
        if (genStr != expected) {
          val diff = {
            var i = 0
            while (i < math.min(genStr.length, expected.length) && genStr(i) == expected(i)) {
              i += 1
            }
            val surround = 50
            val longerStr = if (genStr.length >= expected.length) genStr else expected
            val substring =
              longerStr.substring(math.max(0, i - surround), i) ++
              s"|->${longerStr(i)}<-|" ++
              longerStr.substring(math.min(i + 1, longerStr.length), math.min(i + surround, longerStr.length))

            s"The difference is at character $i: " +
              s"($substring). line: ${ longerStr.substring(0, i).count(_ == '\n') + 1 }"
          }

          val msg =
            s"""
               |The generated file ${gen.getName} did not match gold file
               |"$goldFilesRoot/$suffix".
               |Generated string is ${genStr.length} characters long
               |Expected string is ${expected.length} characters long
               |
               |$diff
               |
               |Compare the output in stdout to the gold file and
               |either fix the generator or update the gold file to match.
             """.stripMargin
          println(msg)
          println(s"Generated file $suffix:\n$genStr<<<EOF")
          fail(msg)
        }
      }
    }
  }

}
