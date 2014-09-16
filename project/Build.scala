import sbt._
import Keys._
import bintray.Plugin._
import bintray.Keys._
import com.typesafe.sbt.SbtSite.site
import com.typesafe.sbt.site.SphinxSupport.Sphinx
import net.virtualvoid.sbt.cross.CrossPlugin

object Scrooge extends Build {
  val libVersion = "3.16.3"
  val utilVersion = "6.19.0"
  val finagleVersion = "6.20.0"

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
    crossScalaVersions := Seq("2.9.2", "2.10.4"),
    scalaVersion := "2.9.2",

    resolvers ++= Seq(
      "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
    ),

    publishM2Configuration <<= (packagedArtifacts, checksums in publish, ivyLoggingLevel) map { (arts, cs, level) =>
      Classpaths.publishConfig(arts, None, resolverName = m2Repo.name, checksums = cs, logging = level)
    },
    publishM2 <<= Classpaths.publishTask(publishM2Configuration, deliverLocal),
    otherResolvers += m2Repo,

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      "junit" % "junit" % "4.8.1" % "test"
    ),
    resolvers += "twitter-repo" at "http://maven.twttr.com",

    scalacOptions ++= Seq("-encoding", "utf8"),
    scalacOptions += "-deprecation",
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
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
  )

  val jmockSettings = Seq(
    libraryDependencies ++= Seq(
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "org.hamcrest" % "hamcrest-all" % "1.1" % "test",
      "org.mockito" % "mockito-all" % "1.9.0" % "test"
    )
  )

  lazy val crossBuildSettings: Seq[Setting[_]] = CrossPlugin.crossBuildingSettings ++ CrossBuilding.scriptedSettings ++ Seq(
    CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")
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
      jmockSettings
  ).settings(
    name := "scrooge-generator",
    libraryDependencies ++= Seq(
      util("core"),
      util("codec"),
      "org.apache.thrift" % "libthrift" % "0.8.0",
      "com.github.scopt" %% "scopt" % "2.1.0",
      "com.novocode" % "junit-interface" % "0.8" % "test->default",
      "com.github.spullara.mustache.java" % "compiler" % "0.8.12",
      "org.codehaus.plexus" % "plexus-utils" % "1.5.4",
      "com.google.code.findbugs" % "jsr305" % "1.3.9",
      "commons-cli" % "commons-cli" % "1.2",
      finagle("core"),
      finagle("thrift") % "test"
    )
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
    )
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
    )
  ).dependsOn(scroogeRuntime)

  lazy val scroogeSbtPlugin = Project(
    id = "scrooge-sbt-plugin",
    base = file("scrooge-sbt-plugin"),
    settings = Project.defaultSettings ++
      sharedSettings ++
      crossBuildSettings ++
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
      sharedSettings
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
