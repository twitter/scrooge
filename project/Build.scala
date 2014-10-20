import sbt._
import Keys._
import bintray.Plugin._
import bintray.Keys._
import com.typesafe.sbt.SbtSite.site
import com.typesafe.sbt.site.SphinxSupport.Sphinx
import net.virtualvoid.sbt.graph.Plugin.graphSettings // For dependency-graph
import sbtassembly.Plugin._
import AssemblyKeys._

object Scrooge extends Build {
  val libVersion = "3.17.0"
  val utilVersion = "6.22.0"
  val finagleVersion = "6.22.0"

  def util(which: String) = "com.twitter" %% ("util-"+which) % utilVersion
  def finagle(which: String) = "com.twitter" %% ("finagle-"+which) % finagleVersion

  val compileThrift = TaskKey[Seq[File]](
    "compile-thrift", "generate thrift needed for tests")

  lazy val publishM2Configuration =
    TaskKey[PublishConfiguration]("publish-m2-configuration",
      "Configuration for publishing to the .m2 repository.")

  lazy val publishM2 =
    TaskKey[Unit]("publish-m2",
      "Publishes artifacts to the .m2 repository.")

  lazy val m2Repo =
    Resolver.file("publish-m2-local",
      Path.userHome / ".m2" / "repository")

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

  val thriftSettings: Seq[Setting[_]] = Seq(
    compileThrift <<= (
      streams,
      baseDirectory,
      fullClasspath in Runtime,
      sourceManaged
    ) map { (out, base, cp, outputDir) =>
      val cmd = "%s %s %s %s".format(
        (base / "src" / "scripts" / "gen-test-thrift").getAbsolutePath,
        cp.files.absString,
        outputDir.getAbsolutePath,
        base.getAbsolutePath)

      out.log.info(cmd)
      cmd ! out.log

      (outputDir ** "*.scala").get.toSeq ++
      (outputDir ** "*.java").get.toSeq
    },
    sourceGenerators <+= compileThrift
  )

  val sharedSettings = Seq(
    version := libVersion,
    organization := "com.twitter",
    crossScalaVersions := Seq("2.10.4"),
    scalaVersion := "2.10.4",

    resolvers ++= Seq(
      "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
    ),

    publishM2Configuration <<= (packagedArtifacts, checksums in publish, ivyLoggingLevel) map { (arts, cs, level) =>
      Classpaths.publishConfig(
        artifacts = arts,
        ivyFile = None,
        resolverName = m2Repo.name,
        checksums = cs,
        logging = level,
        overwrite = true)
    },
    publishM2 <<= Classpaths.publishTask(publishM2Configuration, deliverLocal),
    otherResolvers += m2Repo,

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.2" % "test",
      "junit" % "junit" % "4.10" % "test" exclude("org.mockito", "mockito-all")
    ),
    resolvers += "twitter-repo" at "http://maven.twttr.com",

    scalacOptions ++= Seq("-encoding", "utf8"),
    scalacOptions += "-deprecation",
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint:unchecked"),
    javacOptions in doc := Seq("-source", "1.6"),

    // Sonatype publishing
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    publishMavenStyle := true,
    pomExtra := (
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
      </developers>),
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
        val contents = (
          "name=%s\nversion=%s\nbuild_revision=%s\nbuild_name=%s"
        ).format(name, ver, buildRev, buildName)
        IO.write(file, contents)
        Seq(file)
      }
  ) ++ graphSettings

  val jmockSettings = Seq(
    libraryDependencies ++= Seq(
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test"
    )
  )

  lazy val scrooge = Project(
    id = "scrooge",
    base = file("."),
    settings = Project.defaultSettings ++
      sharedSettings
  ).aggregate(
    scroogeGenerator, scroogeCore,
    scroogeRuntime, scroogeSerializer, scroogeOstrich,
    scroogeLinter
  )

  lazy val scroogeGenerator = Project(
    id = "scrooge-generator",
    base = file("scrooge-generator"),
    settings = Project.defaultSettings ++
      inConfig(Test)(thriftSettings) ++
      sharedSettings ++
      assemblySettings ++
      jmockSettings
  ).settings(
    name := "scrooge-generator",
    libraryDependencies ++= Seq(
      util("core") exclude("org.mockito", "mockito-all"),
      util("codec") exclude("org.mockito", "mockito-all"),
      "org.apache.thrift" % "libthrift" % "0.8.0",
      "com.github.scopt" %% "scopt" % "2.1.0",
      "com.novocode" % "junit-interface" % "0.8" % "test->default" exclude("org.mockito", "mockito-all"),
      "com.github.spullara.mustache.java" % "compiler" % "0.8.12",
      "org.codehaus.plexus" % "plexus-utils" % "1.5.4",
      "com.google.code.findbugs" % "jsr305" % "1.3.9",
      "commons-cli" % "commons-cli" % "1.2",
      finagle("core") exclude("org.mockito", "mockito-all"),
      finagle("thrift") % "test"
    ),
    test in assembly := {},  // Skip tests when running assembly.
    mainClass in assembly := Some("com.twitter.scrooge.Main")
  ).dependsOn(scroogeRuntime % "test")

  lazy val scroogeCore = Project(
    id = "scrooge-core",
    base = file("scrooge-core"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-core",
    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % "0.8.0" % "provided"
    ),
    crossScalaVersions += "2.11.2"
  )

  lazy val scroogeRuntime = Project(
    id = "scrooge-runtime",
    base = file("scrooge-runtime"),
    settings = Project.defaultSettings ++
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
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-ostrich",
    libraryDependencies ++= Seq(
      finagle("ostrich4"),
      finagle("thriftmux"),
      util("app")
    )
  ).dependsOn(scroogeRuntime)

  lazy val scroogeSerializer = Project(
    id = "scrooge-serializer",
    base = file("scrooge-serializer"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-serializer",
    libraryDependencies ++= Seq(
      util("codec"),
      "org.apache.thrift" % "libthrift" % "0.8.0" % "provided"
    ),
    crossScalaVersions += "2.11.2"
  ).dependsOn(scroogeCore)

  lazy val scroogeSbtPlugin = Project(
    id = "scrooge-sbt-plugin",
    base = file("scrooge-sbt-plugin"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      bintrayPublishSettings
  ).settings(
    sbtPlugin := true,
    publishMavenStyle := false,
    repository in bintray := "sbt-plugins",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayOrganization in bintray := Some("twittercsl")
  ).dependsOn(scroogeGenerator)

  lazy val scroogeLinter = Project(
    id = "scrooge-linter",
    base = file("scrooge-linter"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      assemblySettings
  ).settings(
    name := "scrooge-linter"
  ).dependsOn(scroogeGenerator)

  val benchThriftSettings: Seq[Setting[_]] = Seq(
    compileThrift <<= (
      streams,
      baseDirectory,
      dependencyClasspath,
      sourceManaged
    ) map { (out, base, cp, outputDir) =>
      val cmd = "%s %s %s %s".format(
        (base / "src" / "scripts" / "gen-test-thrift").getAbsolutePath,
        cp.files.absString,
        outputDir.getAbsolutePath,
        base.getAbsolutePath)

      out.log.info(cmd)
      cmd ! out.log

      (outputDir ** "*.scala").get.toSeq ++
      (outputDir ** "*.java").get.toSeq
    },
    sourceGenerators <+= compileThrift
  )

  lazy val scroogeBenchmark = Project(
    id = "scrooge-benchmark",
    base = file("scrooge-benchmark"),
    settings = Project.defaultSettings ++
      inConfig(Compile)(benchThriftSettings) ++
      sharedSettings ++
      dumpClasspathSettings
  ).settings(
    libraryDependencies ++= Seq(
      util("app"),
      "com.google.caliper" % "caliper" % "0.5-rc1"
    )
  ).dependsOn(scroogeGenerator, scroogeRuntime)

  lazy val scroogeDoc = Project(
    id = "scrooge-doc",
    base = file("doc"),
    settings =
      Project.defaultSettings ++
      sharedSettings ++
      site.settings ++
      site.sphinxSupport() ++
      Seq(
        scalacOptions in doc <++= (version).map(v => Seq("-doc-title", "Scrooge", "-doc-version", v)),
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
  lazy val DocTest = config("testdoc") extend(Test)
}
