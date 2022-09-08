package com.twitter.scrooge.cocoa_generator

import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.twitter.scrooge.Main
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.testutil.TempDirectory
import com.twitter.scrooge.testutil.Utils
import com.twitter.scrooge.testutil.Utils.verify
import java.io._

class CocoaGeneratorSpec extends Spec {

  /*
   * Gold files can be regenerated via the command
   * source $ ./pants run scrooge-internal/generator:bin --jvm-run-jvm-program-args="--language cocoa --dest scrooge/scrooge-generator-tests/src/test/resources/cocoa_output/ scrooge/scrooge-generator-tests/src/test/resources/test_thrift/cocoa.thrift"
   */

  val templateFiles = List(
    "cocoa_output/TFNTwitterThriftScribeAnotherTestStruct.h",
    "cocoa_output/TFNTwitterThriftScribeAnotherTestStruct.m",
    "cocoa_output/TFNTwitterThriftScribeTestEnum.h",
    "cocoa_output/TFNTwitterThriftScribeTestStruct.h",
    "cocoa_output/TFNTwitterThriftScribeTestStruct.m",
    "cocoa_output/TFNTwitterThriftScribeTestEmptyStruct.h",
    "cocoa_output/TFNTwitterThriftScribeTestEmptyStruct.m"
  ).map { new File(_) }

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

  def getListOfFiles(d: File): List[File] = {
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  "Cocoa generator" should {
    val tempDir = TempDirectory.create(None)
    val includePath = Utils.getIncludeFilePath("cocoa.thrift", Some("test_thrift"))

    val args = Array[String]("-l", "cocoa", "-d", tempDir.getPath, includePath)
    Main.main(args)
    val generated_flist = getListOfFiles(tempDir)

    "generate some .m .h files" in {
      assert(
        templateFiles.size == generated_flist.size,
        "missing file, generated files are:"
          + generated_flist.toString()
      )
    }

    "generate proper content inside files" in {
      templateFiles.map { tFile =>
        generated_flist.find(_.getName() == tFile.getName()) match {
          case Some(genFile) =>
            verify(getFileContents(tFile.toString()), getFileContents(genFile.toString()))
          case None => fail("generator did not produce " + tFile)
        }
      }
    }
  }
}
