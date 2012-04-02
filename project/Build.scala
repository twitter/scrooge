import sbt._
// import Process._
import Keys._
import com.twitter.sbt._

object Scrooge extends Build {
  // projects that use finagle will provide their own dependent jar.
  val finagleVersion = "1.11.0"
  val utilVersion = "2.0.0"

  lazy val root = Project(
    id = "scrooge",
    base = file("."),
    settings = Project.defaultSettings ++
      StandardProject.newSettings
  ).settings(
    name := "scrooge",
    organization := "com.twitter",
    version := "3.0",

    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % "0.8.0",
      "com.github.scopt" %% "scopt" % "2.0.1",
      "com.twitter" % "util-core_2.9.1" % utilVersion,

      // for tests:
      "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test" withSources(),
      "org.scalatest" % "scalatest_2.9.1" % "1.7.1" % "test",
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "org.hamcrest" % "hamcrest-all" % "1.1" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "com.twitter" % "scrooge-runtime" % "1.1.2" % "test",
      "com.twitter" % "finagle-core_2.9.1" % finagleVersion % "test",
      "com.twitter" % "finagle-ostrich4_2.9.1" % finagleVersion % "test"
    ),

    SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")
  )


//  override def mainClass = Some("com.twitter.scrooge.Main")

//  override def subversionRepository = Some("https://svn.twitter.biz/maven-public")

//  override def releaseBuild = !(projectVersion.toString contains "SNAPSHOT")

  // publish the combined distribution zip, too.
//  def publishZipAction = packageDistTask && task {
//    FileUtilities.copyFile(("dist" / distZipName), outputRootPath / distZipName, log)
//  }
//  lazy val publishZip = publishZipAction

//  override def artifacts = super.artifacts ++ Set(Artifact("scrooge", "zip", "zip"))

//  override lazy val publishLocal = publishZipAction && publishLocalAction
//  override lazy val publish = publishZipAction && publishAction

//  lazy val generateTestScalaThrift =
//    runTask(
//      Some("com.twitter.scrooge.Main"),
//      runClasspath,
//      Array(
//        "--finagle",
//        "--ostrich",
//        "-d", "target/gen-scala",
//        "-l", "scala",
//        "src/test/resources/test.thrift"
//      )
//    ) dependsOn(compile, copyResources)

//  lazy val generateTestJavaThrift =
//    runTask(
//      // Some("com.twitter.scrooge.Main"),
//      runClasspath,
//      Array(
//        "--finagle",
//        "--ostrich",
//        "-d", "target/gen-java",
//        "-l", "java",
//        "src/test/resources/test.thrift"
//      )
//    ) dependsOn(compile, copyResources)
//
//  lazy val generateTestThrift = generateTestScalaThrift && generateTestJavaThrift
//  def generateTestThriftAction = generateTestThrift

//  override def testSourceRoots = super.testSourceRoots +++ ("target" / "gen-scala" ##) // +++ ("target" / "gen-java" ##)
//  override def testCompileAction = super.testCompileAction dependsOn generateTestThrift
}
