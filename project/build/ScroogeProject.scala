import sbt._
import Process._
import com.twitter.sbt._

/**
 * Sbt project files are written in a DSL in scala.
 *
 * The % operator is just turning strings into maven dependency declarations, so lines like
 *     val example = "com.example" % "exampleland" % "1.0.3"
 * mean to add a dependency on exampleland version 1.0.3 from provider "com.example".
 */
class ScroogeProject(info: ProjectInfo) extends StandardServiceProject(info)
  with NoisyDependencies
  with DefaultRepos
  with SubversionPublisher
{
  val scala0Repo = "scala0.net" at "http://scala0.net/repositories/"

  // projects that use finagle will provide their own dependent jar.
  val finagleVersion = "1.8.2"
  val utilVersion = "1.11.1"

  val util_core = "com.twitter" % "util-core" % utilVersion
  val util_eval = "com.twitter" % "util-eval" % utilVersion
  val libThrift = "thrift" % "libthrift" % "0.5.0"
  val cmdLine = "net.scala0" % "scala0.cmdline_2.8.1" % "1.1.1"
  val mustache = "org.monkey" % "mustache" % "1.0.1-SNAPSHOT"

  val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test" withSources()
  val jmock = "org.jmock" % "jmock" % "2.4.0" % "test"
  val hamcrest_all = "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test"
  val asm = "asm" % "asm" % "1.5.3" % "test"
  val objenesis = "org.objenesis" % "objenesis" % "1.1" % "test"
  val scroogeRuntime = "com.twitter" % "scrooge-runtime" % "1.0.0" % "test"
  val finagleOstrich4 = "com.twitter" % "finagle-ostrich4" % finagleVersion % "test"

  override def mainClass = Some("com.twitter.scrooge.Main")

  override def subversionRepository = Some("http://svn.local.twitter.com/maven")
}
