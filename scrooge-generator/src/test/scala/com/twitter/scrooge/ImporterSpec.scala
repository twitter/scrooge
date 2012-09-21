package com.twitter.scrooge.frontend

import com.twitter.scalatest._
import java.io.{File, FileOutputStream}
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ImporterSpec extends FunSpec with TestFolder {
  describe("fileImporter") {
    it("finds files on the path") {
      val folder1 = new File(testFolder, "f1")
      val folder2 = new File(testFolder, "f2")
      folder1.mkdir()
      folder2.mkdir()

      val f = new FileOutputStream(new File(folder2, "a.thrift"))
      f.write("hello".getBytes)
      f.close()

      val importer = Importer(Seq(folder1.getAbsolutePath, folder2.getAbsolutePath))
      val c = importer.apply("a.thrift")
      assert(c.isDefined && c.get.data == "hello")
    }

    it("follows relative links correctly") {
      val folder1 = new File(testFolder, "f1")
      val folder2 = new File(testFolder, "f2")
      folder1.mkdir()
      folder2.mkdir()

      val f = new FileOutputStream(new File(folder2, "a.thrift"))
      f.write("hello".getBytes)
      f.close()

      val importer = Importer(Seq(folder1.getAbsolutePath))
      val c = importer.apply("../f2/a.thrift")
      assert(c.isDefined)
      assert(c.get.data == "hello")
      assert(c.get.importer.canonicalPaths contains folder2.getCanonicalPath)
    }
  }
}
