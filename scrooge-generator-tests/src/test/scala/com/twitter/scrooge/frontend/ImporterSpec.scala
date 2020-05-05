package com.twitter.scrooge.frontend

import com.twitter.scrooge.testutil.{Spec, TempDirectory}
import java.io.{File, FileOutputStream}

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

      importer.getResolvedPath("a.thrift") must be(
        Some(new File(testFolder, "f2/a.thrift").getCanonicalPath)
      )
      importer.getResolvedPath("b.thrift") must be(None)
      importer.getResolvedPath("f2/a.thrift") must be(None)
    }
  }

  "DirImporter" should {
    val testFolder = TempDirectory.create(None)

    "have equality defined according to path" in {
      val aImporter = DirImporter(new File(testFolder, "a"))
      val anotherAImporter = DirImporter(new File(testFolder, "a"))
      val bImporter = DirImporter(new File(testFolder, "b"))

      aImporter must not equal bImporter
      aImporter must equal(anotherAImporter)
    }
  }

  "ZipImporter" should {
    val testFolder = TempDirectory.create(None)
    val stubZipContent = "\120\113\005\006" + ("\000" * 18)

    "have equality defined according to path" in {
      val aZip = new File(testFolder, "a.zip")
      val bZip = new File(testFolder, "b.zip")

      Seq(aZip, bZip).foreach(zip => {
        val f = new FileOutputStream(zip)
        f.write(stubZipContent.getBytes)
        f.close()
      })

      val aImporter = ZipImporter(aZip)
      val anotherAImporter = ZipImporter(aZip)
      val bImporter = ZipImporter(bZip)

      aImporter must not equal bImporter
      aImporter must equal(anotherAImporter)
    }
  }

  "MultiImporter" should {
    val testFolder = TempDirectory.create(None)

    "deduplicate importers that are equal" in {
      val aImporter = DirImporter(new File(testFolder, "a"))
      val anotherAImporter = DirImporter(new File(testFolder, "a"))

      val multiImporter = MultiImporter.deduped(Seq(aImporter, anotherAImporter))
      multiImporter.importers.length must equal(1)
    }

    "not deduplicate importers that are not equal, but maintain order" in {
      val importers =
        Seq(DirImporter(new File(testFolder, "b")), DirImporter(new File(testFolder, "a")))
      val multiImporter = MultiImporter.deduped(importers)
      multiImporter.importers must equal(importers)
    }
  }
}
