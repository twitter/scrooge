import sbt._
import Keys._
import java.io.File

// Runs the freshly-built Scrooge binary (used for integration tests and benchmarking)
// How to run: `sbt genTestThrift` or `sbt genBenchmarkThrift`
object ScroogeRunner {
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

  val androidNamespace =
    """-n thrift.defaults=thrift.android_defaults
      |-n thrift.colors=thrift.android_colors
      |-n thrift.test=thrift.android_test
      |-n thrift.test1=thrift.android_test1
      |-n thrift.test2=thrift.android_test2
      |-n thrift.collision=thrift.android_collision
      |-n thrift.constants=thrift.android_constants
      |-n thrift.def.default=thrift.android_def.default
      |-n foo=android_foo
      |-n bar=android_bar
      |-n com.fake=com.android_fake
      |-n com.twitter.scrooge.integration_scala=com.twitter.scrooge.integration_android
      |--default-java-namespace android_thrift_default_namespace
    """.stripMargin



  sealed abstract class Language(val scroogeName: String, val defaultNamespace: String)
  case object Scala extends Language("scala", "")
  case object ApacheJava extends Language("java", apacheJavaNamespace)
  case object Android extends Language("android", androidNamespace)

  class Runner(out: Keys.TaskStreams, cp: Classpath, outputDir: File) {
    def run(
      language: Language,
      namespace: String,
      finagle: Boolean = true,
      args: String,
      genAdapt: Boolean = true
    ): Unit =  {
      val finagleArg = if(finagle) "--finagle" else ""
      val adaptArg = if (genAdapt) "--gen-adapt" else ""

      val command =
        s"java -cp ${cp.files.absString} com.twitter.scrooge.Main --verbose $finagleArg $adaptArg " +
          s" -d ${outputDir.getAbsolutePath} -l ${language.scroogeName} $namespace $args"

      val result: Int = scala.sys.process.Process(command) ! out.log

      if(result != 0) {
        out.log.error("Scrooge run failed. Tried to run:")
        out.log.error(command)
        throw new RuntimeException("Scrooge run failed.")
      }
    }

    def runScrooge(
      languages: Seq[Language],
      args: String,
      genAdapt: Boolean = true
    ) = languages foreach { lang =>
      run(lang, lang.defaultNamespace, finagle = true, genAdapt = genAdapt, args = args)
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

  val genAdaptiveScroogeTestThrift = TaskKey[Seq[File]]("genAdaptiveScroogeTestThrift",
    "Uses Scrooge to generate code from thrift sources, for use in Adaptive Scrooge tests")

  val genAdaptiveScroogeTestThriftTask = genAdaptiveScroogeTestThrift := {
    val base = baseDirectory.value
    val runner = new Runner(streams.value, dependencyClasspath.value, sourceManaged.value)
    import runner._

    val files = filesInDir(s"$base/src/test/thrift") mkString " "
    runScrooge(Seq(Scala), files)

    filesGenerated
  }

  val genTestThrift = TaskKey[Seq[File]]("genTestThrift",
    "Uses Scrooge to generate code from thrift sources, for use in tests")

  val genTestThriftTask = genTestThrift := {
    val base = baseDirectory.value
    val runner = new Runner(streams.value, dependencyClasspath.value, sourceManaged.value)
    import runner._

    section("defaults/") {
      val files = filesInDir(s"$base/src/test/thrift/defaults") mkString " "
      runScrooge(Seq(ApacheJava, Scala, Android), files)
    }

    section("relative/") {
      val files = filesInDir(s"$base/src/test/thrift/relative").filter(_.contains("include")) mkString " "
      runScrooge(Seq(Scala, Android), files)

      val file = s"$base/src/test/thrift/relative/candy.thrift"
      val importArg =
        s"--include-path $base/src/test/thrift/relative/dir2${File.pathSeparator}$base/src/test/thrift/relative/dir3"
      runScrooge(Seq(Scala, Android), s"$file $importArg")
    }

    val airportThriftFiles = filesInDir(s"$base/src/test/thrift/airport") mkString " "

    section("airport/ for Finagle usage") {
      runScrooge(Seq(Scala, Android), airportThriftFiles)
    }

    section("airport/ for vanilla usage") {
      val scalaVanillaNamespace =
        """-n thrift.test=vanilla.test
          |-n thrift.test1=vanilla.test1
          |-n thrift.test2=vanilla.test2
        """.stripMargin

      val androidVanillaNamespace =
        """-n androidthrift.test=vanilla_android.test
          |-n androidthrift.test1=vanilla_android.test1
          |-n androidthrift.test2=vanilla_android.test2
        """.stripMargin

      run(language = Scala, namespace = scalaVanillaNamespace, finagle = false, args = airportThriftFiles)
      run(language = Android, namespace = androidVanillaNamespace, finagle = false, args = airportThriftFiles)
    }

    section("namespace/ with bar and java_bar as default namespace") {
      val files = filesInDir(s"$base/src/test/thrift/namespace") mkString " "
      run(language = Scala,
        namespace = s"${Scala.defaultNamespace} --default-java-namespace bar",
        args = files)
      run(language = Android,
        namespace = s"${Android.defaultNamespace} --default-java-namespace android_bar",
        args = files)
    }

    section("integration/") {
      val files = filesInDir(s"$base/src/test/thrift/integration") mkString " "
      val androidFiles = (filesInDir(s"$base/src/test/thrift/integration") ++ filesInDir(s"$base/src/test/thrift/android_integration")) mkString " "
      run(language = Scala,
        namespace = s"${Scala.defaultNamespace} -n thrift.test=com.twitter.scrooge.integration_scala",
        args = s"--disable-strict $files")
      run(language = ApacheJava,
        namespace = s"${ApacheJava.defaultNamespace} -n thrift.test=com.twitter.scrooge.integration_apache",
        args = s"--disable-strict $files")
      run(language = Android,
        namespace = s"${Android.defaultNamespace} -n thrift.test=com.twitter.scrooge.integration_android",
        args = s"--disable-strict $androidFiles")

    }

    section("finagle_integration/") {
      val files = filesInDir(s"$base/src/test/thrift/finagle_integration") mkString " "
      run(language = Scala,
        namespace = s"${Scala.defaultNamespace} -n thrift.test=com.twitter.scrooge.finagle_integration.thriftscala",
        args = s"--disable-strict $files")
      run(language = ApacheJava,
        namespace = s"${ApacheJava.defaultNamespace} -n thrift.test=com.twitter.scrooge.finagle_integration.thriftjava",
        args = s"--disable-strict $files")

    }

    section("standalone/") {
      val files = filesInDir(s"$base/src/test/thrift/standalone") mkString " "
      runScrooge(Seq(Scala), files)
      runScrooge(Seq(ApacheJava, Android),
        s"$base/src/test/thrift/standalone/enumSet.thrift $base/src/test/thrift/standalone/exception_fields.thrift")
    }

    section("scala/") {
      val files = filesInDir(s"$base/src/test/thrift/scala") mkString " "
      runScrooge(Seq(ApacheJava, Scala), files)
    }

    section("constant_sets.thrift") {
      val file = s"$base/src/test/thrift/constant_sets.thrift"
      runScrooge(Seq(Scala, Android), file)
    }

    filesGenerated
  }

  val genBenchmarkThrift = TaskKey[Seq[File]]("genBenchmarkThrift",
    "Uses Scrooge to generate sources to use in benchmarking")
  val genBenchmarkThriftTask = genBenchmarkThrift := {
    val base = baseDirectory.value
    val runner = new Runner(streams.value, dependencyClasspath.value, sourceManaged.value)
    import runner._

    section("benchmark/") {
      val files = filesInDir(s"$base/src/main/thrift") mkString " "
      runScrooge(Seq(Scala, Android), files)
    }

    filesGenerated
  }

  val genSerializerTestThrift = TaskKey[Seq[File]]("genSerializerTestThrift",
    "Uses Scrooge to generate sources to use in serializer tests")
  val genSerializerTestThriftTask = genSerializerTestThrift := {
    val base = baseDirectory.value
    val runner = new Runner(streams.value, dependencyClasspath.value, sourceManaged.value)
    import runner._

    section("serializer/") {
      val files = filesInDir(s"$base/src/test/thrift") mkString " "
      runScrooge(Seq(Scala), files)
    }

    filesGenerated
  }
}
