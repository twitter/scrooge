package com.twitter.scrooge.swift_generator

import collection.JavaConverters._
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.twitter.scrooge.Main
import com.twitter.scrooge.frontend.ResolvedDocument
import com.twitter.scrooge.frontend.TypeResolver
import com.twitter.scrooge.ast.Document
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.testutil.TempDirectory
import com.twitter.scrooge.testutil.Utils.verify
import java.io.FileInputStream
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import scala.collection.concurrent.TrieMap

class SwiftGeneratorSpec extends Spec {

  def getFileContents(resource: String): String = {
    val ccl = Thread.currentThread().getContextClassLoader
    val is = ccl.getResourceAsStream(resource) match {
      case null => new FileInputStream(resource)
      case input: InputStream => input
    }
    val br = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8))
    try CharStreams.toString(br)
    finally br.close()
  }

  def getListOfFiles(d: File): List[java.nio.file.Path] = {
    if (d.exists && d.isDirectory) {
      Files
        .walk(Paths.get(d.getPath))
        .filter(Files.isRegularFile(_))
        .filter(!Files.isHidden(_))
        .iterator().asScala
        .toList
    } else {
      List.empty
    }
  }

  "Swift generator" should {
    val tempDir = TempDirectory.create(None)

    val includePath = Utils.getIncludeFilePath("swift.thrift", Some("test_thrift/swift"))

    val args = Array[String]("-l", "swift", "-d", tempDir.getPath, includePath)
    Main.main(args)
    val output_dir = tempDir.getPath
    val output_flist =
      getListOfFiles(new File(output_dir))
    val generated_flist = getListOfFiles(tempDir)

    "generate some .swift files" in {
      assert(
        output_flist.size == generated_flist.size,
        "missing file, generated files are:"
          + generated_flist.toString()
      )
    }

    "generate proper content inside files" in {
      output_flist.map { tFile =>
        generated_flist.find(_.getFileName == tFile.getFileName) match {
          case Some(genFile) =>
            verify(getFileContents(tFile.toString), getFileContents(genFile.toString))
          case None => fail("generator did not produce " + tFile)
        }
      }
    }
  }

  "Swift generator classes" should {
    val tempDir = TempDirectory.create(None)

    val includePath = Utils.getIncludeFilePath("swift.thrift", Some("test_thrift/swift"))


    val args =
      Array[String](
        "-l",
        "swift",
        "--language-flag",
        "swift-classes",
        "-d",
        tempDir.getPath,
        includePath)
    Main.main(args)
    val output_dir = tempDir.getPath
    val output_flist =
      getListOfFiles(new File(output_dir))
    val generated_flist = getListOfFiles(tempDir)
    "generate some .swift files" in {
      assert(
        output_flist.size == generated_flist.size,
        "missing file, generated files are:"
          + generated_flist.toString()
      )
    }

    "generate proper content inside files" in {
      output_flist.map { tFile =>
        generated_flist.find(_.getFileName == tFile.getFileName) match {
          case Some(genFile) =>
            verify(getFileContents(tFile.toString), getFileContents(genFile.toString))
          case None => fail("generator did not produce " + tFile)
        }
      }
    }
  }

  "Swift generator internal types" should {
    val tempDir = TempDirectory.create(None)

    val includePath = Utils.getIncludeFilePath("swift.thrift", Some("test_thrift/swift"))

    val args =
      Array[String](
        "-l",
        "swift",
        "--language-flag",
        "swift-internal-types",
        "-d",
        tempDir.getPath,
        includePath)
    Main.main(args)
    val output_dir = tempDir.getPath
    val output_flist =
      getListOfFiles(new File(output_dir))
    val generated_flist = getListOfFiles(tempDir)

    "generate some .swift files" in {
      assert(
        output_flist.size == generated_flist.size,
        "missing file, generated files are:"
          + generated_flist.toString()
      )
    }

    "generate proper content inside files" in {
      output_flist.map { tFile =>
        generated_flist.find(_.getFileName == tFile.getFileName) match {
          case Some(genFile) =>
            verify(getFileContents(tFile.toString), getFileContents(genFile.toString))
          case None => fail("generator did not produce " + tFile)
        }
      }
    }
  }

  "Swift generator Obj-C" should {
    val tempDir = TempDirectory.create(None)

    val includePath = Utils.getIncludeFilePath("swift.thrift", Some("test_thrift/swift"))


    val args =
      Array[String](
        "-l",
        "swift",
        "--language-flag",
        "swift-objc TEST",
        "-d",
        tempDir.getPath,
        includePath)
    Main.main(args)
    val output_dir = tempDir.getPath
    val output_flist =
      getListOfFiles(new File(output_dir))
    val generated_flist = getListOfFiles(tempDir)

    "generate some .swift files" in {
      assert(
        output_flist.size == generated_flist.size,
        "missing file, generated files are:"
          + generated_flist.toString()
      )
    }

    "generate proper content inside files" in {
      output_flist.map { tFile =>
        generated_flist.find(_.getFileName == tFile.getFileName) match {
          case Some(genFile) =>
            verify(getFileContents(tFile.toString), getFileContents(genFile.toString))
          case None => fail("generator did not produce " + tFile)
        }
      }
    }
  }

  "Swift generator with alternative type" should {
    val tempDir = TempDirectory.create(None)

    val includePath = Utils.getIncludeFilePath("alternative_type.thrift", Some("test_thrift/swift"))


    val args = Array[String]("-l", "swift", "-d", tempDir.getPath, includePath)
    Main.main(args)
    val output_dir = tempDir.getPath
    val output_flist =
      getListOfFiles(new File(output_dir))
    val generated_flist = getListOfFiles(tempDir)

    "generate some .swift files" in {
      assert(
        output_flist.size == generated_flist.size,
        "missing file, generated files are:"
          + generated_flist.toString()
      )
    }

    "generate proper content inside files" in {
      output_flist.map { tFile =>
        generated_flist.find(_.getFileName == tFile.getFileName) match {
          case Some(genFile) =>
            verify(getFileContents(tFile.toString), getFileContents(genFile.toString))
          case None => fail("generator did not produce " + tFile)
        }
      }
    }
  }

  "Swift Generator arg parsing" should {

    "parse objc and prefix" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq("swift-objc TEST"))

      assert(Some("TEST") == generator.objcPrefix)
      assert(!generator.generateClasses)
    }

    "parse objc and without prefix" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq("swift-objc"))
      val caught = intercept[Exception] {
        generator.objcPrefix
      }
      assert(caught.getMessage contains "Objective-C Prefix not found")
      assert(!generator.generateClasses)
    }

    "parse objc doesn't throw when not passed" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq("swift"))
      assert(generator.objcPrefix.isEmpty)
      assert(!generator.generateClasses)
    }

    "parse use classes" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq("swift-classes"))

      assert(generator.generateClasses)
    }

    "parse use classes is false when not passed" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq("swift"))
      assert(!generator.generateClasses)
    }

    "parse internal types is true when passed" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq("swift-internal-types"))
      assert(!generator.publicInterface)
    }

    "parse internal types is false when not passed" in {
      val generator = new SwiftGenerator(
        ResolvedDocument(Document(Seq(), Seq()), TypeResolver()),
        "swift",
        TrieMap(),
        Seq(""))
      assert(generator.publicInterface)
    }
  }
}
