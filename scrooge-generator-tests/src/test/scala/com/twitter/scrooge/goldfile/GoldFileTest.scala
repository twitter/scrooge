package com.twitter.scrooge.goldfile

import com.twitter.io.Files
import com.twitter.scrooge.Main
import com.twitter.scrooge.testutil.TempDirectory
import com.twitter.scrooge.testutil.Utils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import scala.io.Source

/**
 * Compares the output of a generator to a set of "golden files".
 *
 * This helps to viscerally see how changes to the generator affect the
 * generated code at code review time. Most updates to the gold files are
 * intentional due to the corresponding changes in the generator code or
 * templates.
 */
abstract class GoldFileTest extends AnyFunSuite with BeforeAndAfterAll {

  private var tempDir: File = _
  protected var generatedFiles: Seq[File] = _
  protected var exception: Option[Throwable] = None

  override protected def beforeAll(): Unit =
    try {
      tempDir = TempDirectory.create(None, deleteAtExit = false)
      if (!deleteTempFiles) {
        println(s"Temp dir $tempDir")
      }

      val inputThrifts = testThriftFiles

      val args = Seq(
        "--language",
        language,
        "--finagle",
        "--java-passthrough",
        "--gen-adapt",
        "--dest",
        tempDir.getPath) ++
        languageFlags.flatMap(flag => Seq("--language-flag", flag)) ++
        inputThrifts

      Main.main(args.toArray)
      generatedFiles = generatedFiles(tempDir)
    } catch {
      case ex: Throwable => exception = Some(ex)
    }

  final def relativePath(f: File): String = {
    val root = tempDir.getAbsolutePath
    val b = f.getAbsolutePath
    assert(b.startsWith(root), s"Cannot relativize $b, not under root $root")
    b.substring(root.length + 1, b.length)
  }

  override protected def afterAll(): Unit = {
    if (deleteTempFiles) {
      if (!Files.delete(tempDir))
        fail(s"Failed to delete $tempDir")
    }
  }

  protected def languageFlags: Seq[String] = Seq.empty
  protected def language: String
  protected def deleteTempFiles: Boolean = true

  private def generatedFiles(f: File): Seq[File] = {
    def accumulate(f: File, buf: Vector[File]): Vector[File] = {
      if (f.isFile) {
        buf :+ f
      } else {
        var bb = buf
        f.listFiles.foreach { f2 => bb = accumulate(f2, bb) }
        bb
      }
    }
    accumulate(f, Vector.empty)
  }

  protected def testThriftFiles = Seq(
    "scrooge/scrooge-generator-tests/src/test/resources/gold_file_input/gold.thrift")
  protected def goldFilesRoot: String =
    s"scrooge/scrooge-generator-tests/src/test/resources/gold_file_output_$language"

  protected def diff(
    gold: InputStream,
    gen: InputStream,
    genFileName: String,
    genRelPath: String
  ): Unit = {
    val genStr = Utils.normalizeHeaders(inputStreamToString(gen))
    val expected = Utils.normalizeHeaders(inputStreamToString(gold))
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
            longerStr.substring(
              math.min(i + 1, longerStr.length),
              math.min(i + surround, longerStr.length))

        s"The difference is at character $i: " +
          s"($substring). line: ${longerStr.substring(0, i).count(_ == '\n') + 1}"
      }

      val msg =
        s"""
           |The generated file ${genFileName} did not match gold file
           |"$goldFilesRoot/$genRelPath".
           |Generated string is ${genStr.length} characters long
           |Expected string is ${expected.length} characters long
           |
           |$diff
           |ScalaGeneratorSpec.scala
           |Compare the output in stdout to the gold file and
           |either fix the generator or update the gold file to match.
           |
           |To regenerate file automatically first remove all the existing files:
           |rm -r $$SCROOGE_ROOT/scrooge-generator-tests/src/test/resources/gold_file_output_$language/*
           |Then regenerate data by passing these arguments into scrooge
           |--language $language --finagle --java-passthrough --gen-adapt --dest $$SCROOGE_ROOT/scrooge-generator-tests/src/test/resources/gold_file_output_$language/ $$SCROOGE_ROOT/scrooge-generator-tests/src/test/resources/gold_file_input/gold.thrift
           |
         """.stripMargin
      println(msg)
      println(s"Generated file $genRelPath:\n$genStr<<<EOF")
      fail(if (deleteTempFiles) msg else msg + s"\nTemp dir $tempDir\n")
    }
  }

  private def inputStreamToString(is: InputStream): String = {
    if (is == null) {
      ""
    } else {
      Source.fromInputStream(is, "UTF-8").mkString
    }
  }

  test("generated output looks as expected") {
    if (exception.isEmpty) {
      generatedFiles.foreach { gen =>
        // We want to take the path after tempDir and compare to the gold file
        // in our resources dir. The +1 removes what would be a leading slash
        // from the relative path.
        val genRelPath = gen.toString.drop(tempDir.toString.length + 1)

        withClue(genRelPath) {
          val genStream = new ByteArrayInputStream(Files.readBytes(gen))
          val goldStream = new java.io.FileInputStream(
            new java.io.File(s"$goldFilesRoot/$genRelPath")
          )
          diff(goldStream, genStream, gen.getName, genRelPath)
        }
      }
    }
  }

  test("gold.thrift is successfully parsed") {
    exception match {
      case Some(t) => fail(t)
      case None => // passed
    }
  }
}
