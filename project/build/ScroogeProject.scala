import sbt._
import Process._
import com.twitter.sbt._

class ScroogeProject(info: ProjectInfo) extends StandardServiceProject(info)
  with NoisyDependencies
  with DefaultRepos
  with SubversionPublisher
{
  // projects that use finagle will provide their own dependent jar.
  val finagleVersion = "1.9.5"
  val utilVersion = "1.11.1"

  val libThrift = "thrift" % "libthrift" % "0.5.0"
  val scopt = "com.github.scopt" %% "scopt" % "1.1.1"
  val util_core = "com.twitter" % "util-core" % utilVersion

  // for tests:
  val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test" withSources()
  val scalatest = "org.scalatest" % "scalatest_2.8.1" % "1.5.1" % "test"
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"
  val scroogeRuntime = "com.twitter" % "scrooge-runtime" % "1.0.2" % "test"
  val util_eval = "com.twitter" % "util-eval" % utilVersion % "test"
  val finagleOstrich4 = "com.twitter" % "finagle-ostrich4" % finagleVersion % "test"

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
}
