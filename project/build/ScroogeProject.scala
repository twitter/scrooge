import sbt._
import Process._
import com.twitter.sbt._

class ScroogeProject(info: ProjectInfo) extends StandardServiceProject(info)
  with NoisyDependencies
  with DefaultRepos
  with SubversionPublisher
{
  // projects that use finagle will provide their own dependent jar.
  val finagleVersion = "1.11.0"
  val utilVersion = "1.12.13"

  val libThrift = "org.apache.thrift" % "libthrift" % "0.8.0"
  val scopt = "com.github.scopt" %% "scopt" % "1.1.3"
  val util_core = "com.twitter" % "util-core_2.9.1" % utilVersion

  // for tests:
  val specs = "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test" withSources()
  val scalatest = "org.scalatest" % "scalatest_2.9.1" % "1.7.1" % "test"
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"
  val scroogeRuntime = "com.twitter" % "scrooge-runtime" % "1.1.2" % "test"
  val finagleCore = "com.twitter" % "finagle-core_2.9.1" % finagleVersion % "test"
  val finagleOstrich4 = "com.twitter" % "finagle-ostrich4_2.9.1" % finagleVersion % "test"

  override def mainClass = Some("com.twitter.scrooge.Main")

  override def subversionRepository = Some("https://svn.twitter.biz/maven-public")

  override def releaseBuild = !(projectVersion.toString contains "SNAPSHOT")

  // publish the combined distribution zip, too.
  def publishZipAction = packageDistTask && task {
    FileUtilities.copyFile(("dist" / distZipName), outputRootPath / distZipName, log)
  }
  lazy val publishZip = publishZipAction

  override def artifacts = super.artifacts ++ Set(Artifact("scrooge", "zip", "zip"))

  override lazy val publishLocal = publishZipAction && publishLocalAction
  override lazy val publish = publishZipAction && publishAction

  lazy val generateTestThrift =
    runTask(
      Some("com.twitter.scrooge.Main"),
      runClasspath,
      Array(
        "--finagle",
        "--ostrich",
        "-d", "target/gen-scala",
        "src/test/resources/test.thrift"
      )
    ) dependsOn(compile, copyResources)

  def generateTestThriftAction = generateTestThrift

  override def testSourceRoots = super.testSourceRoots +++ ("target" / "gen-scala" ##)
  override def testCompileAction = super.testCompileAction dependsOn generateTestThrift
}
