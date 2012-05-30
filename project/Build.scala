import sbt._
import Keys._
import com.twitter.sbt._

object Scrooge extends Build {
  // projects that use finagle will provide their own dependent jar.
  val finagleVersion = "5.0.0"
  val utilVersion = "5.0.3"

  val generateTestThrift = TaskKey[Seq[File]](
    "generate-test-thrift",
    "generate scala/java code used for unit tests"
  )

  val sharedSettings = Seq(
    version := "3.0.2-SNAPSHOT",
    scalaVersion := "2.9.2",

    SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public"),

    generateTestThrift <<= (
      streams,
      exportedProducts in Compile,
      fullClasspath in Runtime,
      sourceManaged in Test,
      resourceDirectory in Test
    ) map { (out, products, cp, managed, resources) =>
      generateThriftFor("scala", cp, managed, resources, out.log) ++
        generateThriftFor("java", cp, managed, resources, out.log)
    }
  )

  lazy val scrooge = Project(
    id = "scrooge",
    base = file("."),
    settings = Project.defaultSettings ++ StandardProject.newSettings ++ sharedSettings
  ) aggregate(scroogeRuntime, scroogeGenerator)

  lazy val scroogeRuntime = Project(
    id = "scrooge-runtime",
    base = file("scrooge-runtime"),
    settings = Project.defaultSettings ++
      StandardProject.newSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-runtime",
    organization := "com.twitter",

    libraryDependencies <<= (scalaVersion, libraryDependencies) { (version, deps) =>
      deps ++ Seq(
        "org.apache.thrift" % "libthrift" % "0.8.0" % "provided",
        "com.twitter" % "util-codec" % utilVersion % "provided",

        // for tests:
        "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test"
      )
    }
  )

  lazy val scroogeGenerator = Project(
    id = "scrooge-generator",
    base = file("scrooge-generator"),
    settings = Project.defaultSettings ++
      StandardProject.newSettings ++
      sharedSettings ++
      // package the distribution zipfile too:
      addArtifact(Artifact("scrooge", "zip", "zip"), PackageDist.packageDist).settings
  ).settings(
    name := "scrooge",
    organization := "com.twitter",

    // we only generate one scrooge to bind them all.
    crossPaths := false,

    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % "0.8.0",
      "com.github.scopt" % "scopt_2.9.1" % "2.0.1",
      "com.twitter" % "util-core" % utilVersion,

      // for tests:
      "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test" withSources(),
      "org.scalatest" % "scalatest_2.9.1" % "1.7.1" % "test",
      "com.twitter" % "scalatest-mixins_2.9.1" % "1.0.3" % "test",
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "org.hamcrest" % "hamcrest-all" % "1.1" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "com.twitter" % "finagle-core" % finagleVersion % "test",
      "com.twitter" % "finagle-thrift" % finagleVersion % "test",
      "com.twitter" % "finagle-ostrich4" % finagleVersion % "test"
    ),

    mainClass := Some("com.twitter.scrooge.Main"),

    (sourceGenerators in Test) <+= generateTestThrift
  ) dependsOn(scroogeRuntime)

  def generateThriftFor(
    language: String,
    classpath: Classpath,
    managedFolder: File,
    resourceFolder: File,
    log: Logger
  ): Seq[File] = {
    val outFolder = managedFolder / language
    outFolder.mkdirs()

    val extraArgs = if (language != "scala") {
      Seq(
        "-n", "thrift.test=thrift." + language + "_test",
        "-n", "thrift.test1=thrift." + language + "_test1",
        "-n", "thrift.test2=thrift." + language + "_test2"
      )
    } else {
      Seq()
    }
    log.info("Generating " + language + " files for tests ...")
    val command = List(
      "java",
      "-cp", classpath.files.mkString(":"),
      "com.twitter.scrooge.Main",
      "--verbose",
      "--finagle",
      "--ostrich",
      "-d", outFolder.getAbsolutePath.toString,
      "-l", language
    ) ++ extraArgs ++ (resourceFolder ** "*.thrift").get.map(_.toString)
    log.debug(command.mkString(" "))
    command ! log
    (outFolder ** ("*." + language)).get.toSeq
  }
}
