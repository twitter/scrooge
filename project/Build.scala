import sbt._
import Keys._

object Scrooge extends Build {
  val utilVersion = "6.3.4"
  val finagleVersion = "6.4.0"

  def util(which: String) = "com.twitter" %% ("util-"+which) % utilVersion
  def finagle(which: String) = "com.twitter" %% ("finagle-"+which) % finagleVersion

  val compileThrift = TaskKey[Seq[File]](
    "compile-thrift", "generate thrift needed for tests")

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
    version := "3.1.2",
    organization := "com.twitter",
    crossScalaVersions := Seq("2.9.2", "2.10.0"),

    resolvers ++= Seq(
      "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
    ),

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" %"1.9.1" % "test",
      "org.scala-tools.testing" %% "specs" % "1.6.9" % "test" withSources() cross CrossVersion.binaryMapped {
        case "2.9.2" => "2.9.1"
        case "2.10.0" => "2.10"
        case x => x
      },
      "junit" % "junit" % "4.8.1" % "test",
      "com.novocode" % "junit-interface" % "0.8" % "test->default"
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
    }
  )

  val jmockSettings = Seq(
    libraryDependencies ++= Seq(
      "org.jmock" % "jmock" % "2.4.0" % "test",
      "cglib" % "cglib" % "2.1_3" % "test",
      "asm" % "asm" % "1.5.3" % "test",
      "org.objenesis" % "objenesis" % "1.1" % "test",
      "org.hamcrest" % "hamcrest-all" % "1.1" % "test"
    )
  )

  lazy val scrooge = Project(
    id = "scrooge",
    base = file("."),
    settings = Project.defaultSettings ++
      sharedSettings
  ).aggregate(
    scroogeGenerator, scroogeMavenPlugin, scroogeRuntime
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
      finagle("core") % "test",
      finagle("thrift") % "test",
      finagle("ostrich4") % "test"
    )
  ).dependsOn(scroogeRuntime % "test")

  lazy val scroogeMavenPlugin = Project(
    id = "scrooge-maven-plugin",
    base = file("scrooge-maven-plugin"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-maven-plugin",
    libraryDependencies ++= Seq(
      "org.apache.maven" % "maven-plugin-api" % "2.0.9",
      "com.google.collections" % "google-collections" % "0.8",
      "org.codehaus.plexus" % "plexus-utils" % "1.5.4",
      "org.apache.maven" % "maven-project" % "2.0.9",
      "org.slf4j" % "slf4j-api" % "1.6.1"
    )
  ).dependsOn(scroogeGenerator)

  lazy val scroogeRuntime = Project(
    id = "scrooge-runtime",
    base = file("scrooge-runtime"),
    settings = Project.defaultSettings ++
      sharedSettings
  ).settings(
    name := "scrooge-runtime",
    libraryDependencies ++= Seq(
      util("codec"),
      finagle("core"),
      finagle("thrift"),
      "org.apache.thrift" % "libthrift" % "0.8.0" % "provided"
    )
  )
}
