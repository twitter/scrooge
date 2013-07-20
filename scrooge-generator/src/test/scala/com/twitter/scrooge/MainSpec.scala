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
}
