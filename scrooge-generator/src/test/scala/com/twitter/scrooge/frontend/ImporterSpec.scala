package com.twitter.scrooge.frontend

import java.io.{File, FileOutputStream}
import org.specs.SpecificationWithJUnit
import com.twitter.scrooge.testutil.TempDirectory

class ImporterSpec extends SpecificationWithJUnit {
  "fileImporter" should {
    val testFolder = TempDirectory.create(None)

    "finds files on the path" in {
      val folder1 = new File(testFolder, "f1")
      val folder2 = new File(testFolder, "f2")
      folder1.mkdir()
      folder2.mkdir()

      val f = new FileOutputStream(new File(folder2, "a.thrift"))
      f.write("hello".getBytes)
      f.close()

      val importer = Importer(Seq(folder1.getAbsolutePath, folder2.getAbsolutePath))
      val c = importer.apply("a.thrift")
      c.isDefined must beTrue
      c.get.data mustEqual "hello"
    }

    "follows relative links correctly" in {
      val folder1 = new File(testFolder, "f1")
      val folder2 = new File(testFolder, "f2")
      folder1.mkdir()
      folder2.mkdir()

      val f = new FileOutputStream(new File(folder2, "a.thrift"))
      f.write("hello".getBytes)
      f.close()

      val importer = Importer(Seq(folder1.getAbsolutePath))
      val c = importer.apply("../f2/a.thrift")
      c.isDefined must beTrue
      c.get.data mustEqual "hello"
      (c.get.importer.canonicalPaths contains folder2.getCanonicalPath) must beTrue
    }
    
    "reads utf-8 data correctly" in {
      val folder1 = new File(testFolder, "f1")
      val folder2 = new File(testFolder, "f2")
      folder1.mkdir()
      folder2.mkdir()

      val f = new FileOutputStream(new File(folder2, "a.thrift"))
      f.write("你好".getBytes("UTF-8"))
      f.close()

      val importer = Importer(Seq(folder1.getAbsolutePath, folder2.getAbsolutePath))
      val c = importer.apply("a.thrift")
      c.isDefined must beTrue
      c.get.data mustEqual "你好"
    }
  }
}
