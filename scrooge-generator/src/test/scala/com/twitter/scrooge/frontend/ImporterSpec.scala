package com.twitter.scrooge.frontend

import java.io.{File, FileOutputStream}
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.testutil.TempDirectory

class ImporterSpec extends Spec {
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
      c.isDefined must be(true)
      c.get.data must be("hello")
      c.get.thriftFilename.get must be("a.thrift")
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
      c.isDefined must be(true)
      c.get.data must be("hello")
      (c.get.importer.canonicalPaths contains folder2.getCanonicalPath) must be(true)
      c.get.thriftFilename.get must be("a.thrift")
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
      c.isDefined must be(true)
      c.get.data must be("你好")
      c.get.thriftFilename.get must be("a.thrift")
    }

    "returns resolved path correctly" in {
      val folder1 = new File(testFolder, "f1")
      val folder2 = new File(testFolder, "f2")
      folder1.mkdir()
      folder2.mkdir()

      val f = new FileOutputStream(new File(folder2, "a.thrift"))
      f.write("hello".getBytes)
      f.close()

      val importer = Importer(Seq(folder1.getAbsolutePath, folder2.getAbsolutePath))

      importer.getResolvedPath("a.thrift") must be(Some(new File(testFolder, "f2/a.thrift").getCanonicalPath))
      importer.getResolvedPath("b.thrift") must be(None)
      importer.getResolvedPath("f2/a.thrift") must be(None)
    }
  }
}
