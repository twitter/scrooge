import sbt._
import Process._
import com.twitter.sbt._

class Project(info: ProjectInfo) extends StandardParentProject(info) {
  val runtimeProject = project(
    "scrooge-runtime", "scrooge-runtime",
    new RuntimeProject(_))

  val generatorProject = project(
    "scrooge-generator", "scrooge-generator",
    new GeneratorProject(_), runtimeProject)

  class RuntimeProject(info: ProjectInfo)
    extends StandardLibraryProject(info)
    with DefaultRepos
    with ProjectDependencies
    with SubversionPublisher
  {
    override def subversionRepository = Some("https://svn.twitter.biz/maven-public")

    buildScalaVersion match {
      case "2.8.1" => {
        projectDependencies(
          "finagle" ~ "finagle-thrift",
          "util" ~ "util-codec"
        )
      }
      case "2.9.1" => {
        projectDependencies(
          "finagle" ~ "finagle-thrift_2.9.1",
          "util" ~ "util-codec_2.9.1"
        )
      }
    }

    // for tests:
    val specs = buildScalaVersion match {
      case "2.8.1" => "org.scala-tools.testing" % "specs_2.8.1" % "1.6.6" % "test"
      case "2.9.1" => "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test"
    }

    override def pomExtra =
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>

    // use "scrooge-runtime_<scalaversion>" as the published name:
    override def disableCrossPaths = false
  }

  class GeneratorProject(info: ProjectInfo)
    extends StandardServiceProject(info)
    with NoisyDependencies
    with DefaultRepos
    with ProjectDependencies
    with SubversionPublisher
  {
    buildScalaVersion match {
      case "2.8.1" => {
        projectDependencies(
          "finagle" ~ "finagle-thrift",
          "util" ~ "util-codec"
        )
      }
      case "2.9.1" => {
        projectDependencies(
          "finagle" ~ "finagle-thrift_2.9.1",
          "util" ~ "util-codec_2.9.1"
        )
      }
    }

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

    lazy val generateTestScalaThrift =
      runTask(
        Some("com.twitter.scrooge.Main"),
        runClasspath,
        Array(
          "--finagle",
          "--ostrich",
          "-d", "scrooge-generator/target/gen-scala",
          "-l", "scala",
          "scrooge-generator/src/test/resources/test.thrift"
        )
      ) dependsOn(compile, copyResources)

    lazy val generateTestJavaThrift =
      runTask(
        Some("com.twitter.scrooge.Main"),
        runClasspath,
        Array(
          "--finagle",
          "--ostrich",
          "-d", "scrooge-generator/target/gen-java",
          "-l", "java",
          "scrooge-generator/src/test/resources/test.thrift"
        )
      ) dependsOn(compile, copyResources)

    lazy val generateTestThrift = generateTestScalaThrift && generateTestJavaThrift
    def generateTestThriftAction = generateTestThrift

    override def testSourceRoots = super.testSourceRoots +++ ("target" / "gen-scala" ##) // +++ ("target" / "gen-java" ##)
    override def testCompileAction = super.testCompileAction dependsOn generateTestThrift
  }
}
