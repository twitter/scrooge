import sbt._
import Keys._
import bintray.Plugin._
import bintray.Keys._
import com.typesafe.sbt.SbtSite.site
import com.typesafe.sbt.site.SphinxSupport.Sphinx
import net.virtualvoid.sbt.graph.Plugin.graphSettings // For dependency-graph
import pl.project13.scala.sbt.JmhPlugin
import sbtassembly.Plugin._
import AssemblyKeys._
import sbtbuildinfo.Plugin._
import scoverage.ScoverageSbtPlugin

object Scrooge extends Build {
  val branch = Process("git" :: "rev-parse" :: "--abbrev-ref" :: "HEAD" :: Nil).!!.trim
  val suffix = if (branch == "master") "" else "-SNAPSHOT"

  val libVersion = "4.5.0" + suffix

  // To build the develop branch you need to publish util, ostrich and finagle locally:
  // 'git checkout develop; sbt publishLocal' to publish SNAPSHOT versions of these projects.
  val utilVersion = "6.32.0" + suffix
  val finagleVersion = "6.33.0" + suffix

  val libthriftVersion = "0.5.0-1"

  def util(which: String) = "com.twitter" %% ("util-"+which) % utilVersion
  def finagle(which: String) = "com.twitter" %% ("finagle-"+which) % finagleVersion

  val compileThrift = TaskKey[Seq[File]](
    "compile-thrift", "generate thrift needed for tests")

  val dumpClasspath = TaskKey[File](
    "dump-classpath", "generate a file containing the full classpath")

  val dumpClasspathSettings: Seq[Setting[_]] = Seq(
    dumpClasspath <<= (
      baseDirectory,
      fullClasspath in Runtime
    ) map { (base, cp) =>
      val file = new File((base / ".classpath.txt").getAbsolutePath)
      val out = new java.io.FileWriter(file)
      try out.write(cp.files.absString) finally out.close()
      file
    }
  )

  val testThriftSettings: Seq[Setting[_]] = Seq(
    sourceGenerators in Test <+= ScroogeRunner.genTestThrift,
    ScroogeRunner.genTestThriftTask
  )

  def scalacOptionsVersion(sv: String): Seq[String] = {
    Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-Xlint",
      "-encoding", "utf8"
    ) ++ (CrossVersion.partialVersion(sv) match {
      case Some((2, x)) if x >= 11 => Seq("-Ypatmat-exhaust-depth", "40")
      case _ => Nil
    })
  }

  val sharedSettings = Seq(
    version := libVersion,
    organization := "com.twitter",
    crossScalaVersions := Seq("2.10.6", "2.11.7"),
    scalaVersion := "2.11.7",

    resolvers ++= Seq(
      "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
    ),

    ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 10)) => false
        case _ => true
      }
    ),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
      "junit" % "junit" % "4.12" % "test"
    ),
    resolvers += "twitter-repo" at "https://maven.twttr.com",

    scalacOptions := scalacOptionsVersion(scalaVersion.value),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked"),
    javacOptions in doc := Seq("-source", "1.7"),

    // Sonatype publishing
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishMavenStyle := true,
    pomExtra :=
      <url>https://github.com/twitter/scrooge</url>
      <licenses>
        <license>
          <name>Apache License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:twitter/scrooge.git</url>
        <connection>scm:git:git@github.com:twitter/scrooge.git</connection>
      </scm>
      <developers>
        <developer>
          <id>twitter</id>
          <name>Twitter Inc.</name>
          <url>https://www.twitter.com/</url>
        </developer>
      </developers>,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    resourceGenerators in Compile <+=
      (resourceManaged in Compile, name, version) map { (dir, name, ver) =>
        val file = dir / "com" / "twitter" / name / "build.properties"
        val buildRev = Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
        val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
        val contents = s"name=$name\nversion=$ver\nbuild_revision=$buildRev\nbuild_name=$buildName"
        IO.write(file, contents)
        Seq(file)
      }
  ) ++ graphSettings

  val jmockSettings = Seq(
    libraryDependencies ++= Seq(
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "cglib" % "cglib" % "2.2.2" % "test",
      "asm" % "asm" % "3.3.1" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test"
    )
  )

  lazy val scrooge = Project(
    id = "scrooge",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).aggregate(
    scroogeGenerator, scroogeCore,
    scroogeRuntime, scroogeSerializer, scroogeOstrich,
    scroogeLinter
  )

  lazy val scroogeGenerator = Project(
    id = "scrooge-generator",
    base = file("scrooge-generator"),
    settings = Defaults.coreDefaultSettings ++
      inConfig(Test)(testThriftSettings) ++
      sharedSettings ++
      assemblySettings ++
      jmockSettings
  ).settings(
    name := "scrooge-generator",
    libraryDependencies ++= Seq(
      util("core") exclude("org.mockito", "mockito-all"),
      util("codec") exclude("org.mockito", "mockito-all"),
      util("logging") exclude("org.mockito", "mockito-all"),
      "org.apache.thrift" % "libthrift" % libthriftVersion,
      "com.github.scopt" %% "scopt" % "3.3.0",
      "com.novocode" % "junit-interface" % "0.8" % "test->default" exclude("org.mockito", "mockito-all"),
      "com.github.spullara.mustache.java" % "compiler" % "0.8.18",
      "org.codehaus.plexus" % "plexus-utils" % "1.5.4",
      "org.slf4j" % "slf4j-log4j12" % "1.7.7" % "test", // used in thrift transports
      "com.google.code.findbugs" % "jsr305" % "2.0.1",
      "commons-cli" % "commons-cli" % "1.2",
      finagle("thrift") % "test"
    ),
    test in assembly := {},  // Skip tests when running assembly.
    mainClass in assembly := Some("com.twitter.scrooge.Main")
  ).dependsOn(scroogeRuntime % "test")

  lazy val scroogeCore = Project(
    id = "scrooge-core",
    base = file("scrooge-core"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-core",
    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % libthriftVersion % "provided"
    )
  )

  lazy val scroogeRuntime = Project(
    id = "scrooge-runtime",
    base = file("scrooge-runtime"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-runtime",
    libraryDependencies ++= Seq(
      finagle("thrift")
    )
  ).dependsOn(scroogeCore)

  lazy val scroogeOstrich = Project(
    id = "scrooge-ostrich",
    base = file("scrooge-ostrich"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-ostrich",
    libraryDependencies ++= Seq(
      finagle("ostrich4"),
      finagle("thriftmux"),
      util("app")
    )
  ).dependsOn(scroogeRuntime)

  val serializerTestThriftSettings: Seq[Setting[_]] = Seq(
    sourceGenerators <+= ScroogeRunner.genSerializerTestThrift,
    ScroogeRunner.genSerializerTestThriftTask
  )

  lazy val scroogeSerializer = Project(
    id = "scrooge-serializer",
    base = file("scrooge-serializer"),
    settings = Defaults.coreDefaultSettings ++
      inConfig(Test)(serializerTestThriftSettings) ++
      sharedSettings
  ).settings(
    name := "scrooge-serializer",
    libraryDependencies ++= Seq(
      util("app"),
      util("codec"),
      "org.slf4j" % "slf4j-log4j12" % "1.7.7" % "test",
      "org.apache.thrift" % "libthrift" % libthriftVersion % "provided"
    )
  ).dependsOn(scroogeCore, scroogeGenerator % "test")

  lazy val scroogeSbtPlugin = Project(
    id = "scrooge-sbt-plugin",
    base = file("scrooge-sbt-plugin"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings ++
      bintrayPublishSettings ++
      buildInfoSettings
  ).settings(
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.twitter",
      sbtPlugin := true,
      publishMavenStyle := false,
      repository in bintray := "sbt-plugins",
      licenses += (("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))),
      bintrayOrganization in bintray := Some("twittercsl")
  ).dependsOn(scroogeGenerator)

  lazy val scroogeLinter = Project(
    id = "scrooge-linter",
    base = file("scrooge-linter"),
    settings = Defaults.coreDefaultSettings ++
      sharedSettings ++
      assemblySettings
  ).settings(
    name := "scrooge-linter"
  ).dependsOn(scroogeGenerator)

  val benchThriftSettings: Seq[Setting[_]] = Seq(
    sourceGenerators <+= ScroogeRunner.genBenchmarkThrift,
    ScroogeRunner.genBenchmarkThriftTask
  )

  lazy val scroogeBenchmark = Project(
    id = "scrooge-benchmark",
    base = file("scrooge-benchmark"),
    settings = Defaults.coreDefaultSettings ++
      inConfig(Compile)(benchThriftSettings) ++
      sharedSettings ++
      dumpClasspathSettings ++
      JmhPlugin.projectSettings
  )
  .enablePlugins(JmhPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-log4j12" % "1.7.7", // Needed for the thrift transports
      "org.apache.thrift" % "libthrift" % libthriftVersion
    )
  ).dependsOn(scroogeGenerator, scroogeRuntime, scroogeSerializer)

  lazy val scroogeDoc = Project(
    id = "scrooge-doc",
    base = file("doc"),
    settings =
      Defaults.coreDefaultSettings ++
      sharedSettings ++
      site.settings ++
      site.sphinxSupport() ++
      Seq(
        scalacOptions in doc <++= version.map(v => Seq("-doc-title", "Scrooge", "-doc-version", v)),
        includeFilter in Sphinx := ("*.html" | "*.png" | "*.js" | "*.css" | "*.gif" | "*.txt")
      )
    ).configs(DocTest).settings(
      inConfig(DocTest)(Defaults.testSettings): _*
    ).settings(
      unmanagedSourceDirectories in DocTest <+= baseDirectory { _ / "src/sphinx/code" },

      // Make the "test" command run both, test and doctest:test
      test <<= Seq(test in Test, test in DocTest).dependOn
    )

  /* Test Configuration for running tests on doc sources */
  lazy val DocTest = config("testdoc") extend Test
}
