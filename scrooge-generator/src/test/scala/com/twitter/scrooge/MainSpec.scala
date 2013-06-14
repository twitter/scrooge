package com.twitter.scrooge

import java.io.{FileWriter, File}
import scala.io.Source
import org.specs.SpecificationWithJUnit
import com.twitter.scrooge.testutil.TempDirectory

class MainSpec extends SpecificationWithJUnit {
  "Scrooge Main" should {
    "gen file mapping with absolute paths" in {
      val inDir = TempDirectory.create(None)
      val outDir = TempDirectory.create(None)
      val manifestDir = TempDirectory.create(None)
      test(inDir, outDir, manifestDir)
    }

    "gen file mapping with relative paths" in {
      val inDir = TempDirectory.create(Some(new File(".")))
      val outDir = TempDirectory.create(Some(new File(".")))
      val manifestDir = TempDirectory.create(Some(new File(".")))
      test(inDir, outDir, manifestDir)
    }
  }

  private def buildPath(segments: String*) = segments.mkString(File.separator)

  private def test(inDir:File, outDir: File, manifestDir: File){
    val input = new File(inDir, "input.thrift")
    val fw = new FileWriter(input)
    fw.write("""
namespace java MyTest
const string tyrion = "lannister"
typedef list<i32> Ladder
enum Direction { NORTH, SOUTH, EAST=90, WEST }
struct Point {
  1: double x
  /** comments*/
  2: double y
}
             """)
    fw.close()
    val fileMap = new File(manifestDir, "map.txt")
    val args = Array[String](
      "-d", outDir.getPath,
      "--gen-file-map", fileMap.getPath,
      input.getPath)

    Main.main(args)
    val manifestString = Source.fromFile(fileMap).mkString
    val expected =
      input.getPath + " -> " + buildPath(outDir.getPath, "MyTest", "Constants.scala") + "\n" +
        input.getPath + " -> " + buildPath(outDir.getPath, "MyTest", "Direction.scala") + "\n" +
        input.getPath + " -> " + buildPath(outDir.getPath, "MyTest", "Point.scala")+ "\n"
    manifestString mustEqual expected
  }

  "Scrooge Compiler" should {
    "handle import mappings" in {
      val inDir1 = TempDirectory.create(Some(new File(".")))
      val mapped1 = new File(inDir1, "mapped1.thrift")
      val mapped1F = new FileWriter(mapped1)
      mapped1F.write("""
include "mapped2.thrift"
      """)
      mapped1F.close()

      val inDir2 = TempDirectory.create(Some(new File(".")))
      val mapped2 = new File(inDir2, "mapped2.thrift")
      val mapped2F = new FileWriter(mapped2)
      mapped2F.write("""
struct Foo {}
      """)
      mapped2F.close()

      val outDir = TempDirectory.create(Some(new File(".")))

      val compiler = new Compiler()
      compiler.includeMappings += ("mapped2.thrift" -> inDir2.getPath)
      compiler.destFolder = outDir.getPath
      compiler.thriftFiles ++= List(mapped1.getPath, mapped2.getPath)
      compiler.run()
    }
  }
}
