import sbt._
import Keys._
import java.io.File

// Runs the freshly-built Scrooge binary (used for integration tests and benchmarking)
// How to run: `sbt genTestThrift` or `sbt genBenchmarkThrift`
object ScroogeRunner {
  val javaNamespace =
    """-n thrift.defaults=thrift.java_defaults
      |-n thrift.colors=thrift.java_colors
      |-n thrift.test=thrift.java_test
      |-n thrift.test1=thrift.java_test1
      |-n thrift.test2=thrift.java_test2
      |-n thrift.collision=thrift.java_collision
      |-n thrift.constants=thrift.java_constants
      |-n thrift.def.default=thrift.java_def.default
      |-n foo=java_foo
      |-n bar=java_bar
      |-n com.fake=com.java_fake
      |-n com.twitter.scrooge.integration_scala=com.twitter.scrooge.integration_java
      |--default-java-namespace java_thrift
    """.stripMargin

  val apacheJavaNamespace =
    """-n thrift.defaults=thrift.apache_java_defaults
      |-n thrift.colors=thrift.apache_java_colors
      |-n thrift.test=thrift.apache_java_test
      |-n thrift.test1=thrift.apache_java_test1
      |-n thrift.test2=thrift.apache_java_test2
      |-n thrift.collision=thrift.apache_java_collision
      |-n thrift.def.default=thrift.apache_java_def.default_ns
      |-n foo=apache_java_foo
      |-n bar=apache_java_bar
      |-n com.fake=com.apache_java_fake
      |-n com.twitter.scrooge.integration_scala=com.twitter.scrooge.integration_apache
      |--default-java-namespace apache_java_thrift
    """.stripMargin

  sealed abstract class Language(val scroogeName: String, val defaultNamespace: String)
  case object Scala extends Language("scala", "")
  case object ApacheJava extends Language("java", apacheJavaNamespace)
  case object Java extends Language("experimental-java", javaNamespace)

  class Runner(out: Keys.TaskStreams, cp: Classpath, outputDir: File) {
    def run(language: Language, namespace: String, finagle: Boolean = true, args: String) : Unit =  {
      val finagleArg = if(finagle) "--finagle" else ""

      val command =
        s"java -cp ${cp.files.absString} com.twitter.scrooge.Main --verbose $finagleArg " +
          s" -d ${outputDir.getAbsolutePath} -l ${language.scroogeName} $namespace $args"

      val result: Int = command ! out.log

      if(result != 0) {
        out.log.error("Scrooge run failed. Tried to run:")
        out.log.error(command)
        throw new RuntimeException("Scrooge run failed.")
      }
    }

    def runScrooge(languages: Seq[Language], args: String) = languages foreach { lang =>
      run(lang, lang.defaultNamespace, finagle = true, args = args)
    }

    def section(description: String)(f: => Unit) = {
      out.log.info(s"Running Scrooge on $description")
      f
    }

    def filesInDir(dir: String) = recursiveListFiles(new File(dir)).map(_.toString).filter(_.endsWith(".thrift"))

    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    def filesGenerated : Seq[File] =
      (outputDir ** "*.scala").get.toSeq ++
      (outputDir ** "*.java").get.toSeq

  }

  val genTestThrift = TaskKey[Seq[File]]("genTestThrift",
    "Uses Scrooge to generate code from thrift sources, for use in tests")

  val genTestThriftTask = genTestThrift <<=
    (streams, baseDirectory, dependencyClasspath, sourceManaged) map { (out, base, cp, outputDir) =>
      val runner = new Runner(out, cp, outputDir)
      import runner._

      section("defaults/") {
        val files = filesInDir(s"$base/src/test/thrift/defaults") mkString " "
        runScrooge(Seq(ApacheJava, Scala), files)
      }

      section("relative/") {
        val files = filesInDir(s"$base/src/test/thrift/relative").filter(_.contains("include")) mkString " "
        runScrooge(Seq(Java, Scala), files)

        val file = s"$base/src/test/thrift/relative/candy.thrift"
        val importArg =
          s"-i $base/src/test/thrift/relative/dir2${File.pathSeparator}$base/src/test/thrift/relative/include3.jar"
        runScrooge(Seq(Java, Scala), s"$file $importArg")
      }

      val airportThriftFiles = filesInDir(s"$base/src/test/thrift/airport") mkString " "

      section("airport/ for Finagle usage") {
        runScrooge(Seq(Java, Scala), airportThriftFiles)
      }

      section("airport/ for vanilla usage") {
        val javaVanillaNamespace =
          """-n thrift.test=vanilla_java.test
            |-n thrift.test1=vanilla_java.test1
            |-n thrift.test2=vanilla_java.test2
          """.stripMargin

        val scalaVanillaNamespace =
          """-n thrift.test=vanilla.test
            |-n thrift.test1=vanilla.test1
            |-n thrift.test2=vanilla.test2
          """.stripMargin

        run(language = Java, namespace = javaVanillaNamespace, finagle = false, args = airportThriftFiles)
        run(language = Scala, namespace = scalaVanillaNamespace, finagle = false, args = airportThriftFiles)
      }

      section("namespace/ with bar and java_bar as default namespace") {
        val files = filesInDir(s"$base/src/test/thrift/namespace") mkString " "
        run(language = Java,
          namespace = s"${Java.defaultNamespace} --default-java-namespace java_bar",
          args = files)
        run(language = Scala,
          namespace = s"${Scala.defaultNamespace} --default-java-namespace bar",
          args = files)
      }

      section("integration/") {
        val files = filesInDir(s"$base/src/test/thrift/integration") mkString " "
        run(language = Scala,
          namespace = s"${Scala.defaultNamespace} -n thrift.test=com.twitter.scrooge.integration_scala",
          args = s"--disable-strict $files")
        run(language = Java,
          namespace = s"${Java.defaultNamespace} -n thrift.test=com.twitter.scrooge.integration_java",
          args = s"--disable-strict $files")
        run(language = ApacheJava,
          namespace = s"${ApacheJava.defaultNamespace} -n thrift.test=com.twitter.scrooge.integration_apache",
          args = s"--disable-strict $files")
      }

      section("standalone/") {
        val files = filesInDir(s"$base/src/test/thrift/standalone") mkString " "
        runScrooge(Seq(Java, Scala), files)
        runScrooge(Seq(ApacheJava), s"$base/src/test/thrift/standalone/enumSet.thrift")
      }

      section("constant_sets.thrift") {
        val file = s"$base/src/test/thrift/constant_sets.thrift"
        runScrooge(Seq(Java, Scala), file)
      }

      filesGenerated
    }

  val genBenchmarkThrift = TaskKey[Seq[File]]("genBenchmarkThrift",
    "Uses Scrooge to generate sources to use in benchmarking")
  val genBenchmarkThriftTask = genBenchmarkThrift <<=
    (streams, baseDirectory, dependencyClasspath, sourceManaged) map { (out, base, cp, outputDir) =>
      val runner = new Runner(out, cp, outputDir)
      import runner._

      section("benchmark/") {
        val files = filesInDir(s"$base/src/main/thrift") mkString " "
        runScrooge(Seq(Scala), files)
      }

      filesGenerated
    }
}
