package com.twitter.scrooge.cocoa_generator

import com.twitter.scrooge.Main
import java.io._
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.twitter.scrooge.testutil.{Spec, TempDirectory}
import com.twitter.scrooge.testutil.Utils.verify

class CocoaGeneratorSpec extends Spec {

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

    val ccl = Thread.currentThread().getContextClassLoader
    val inputThrift = ccl.getResource("test_thrift/cocoa.thrift").getPath()

    val args = Array[String]("-l", "cocoa", "-d", tempDir.getPath, inputThrift)
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
