package com.twitter.scrooge

import sbt._
import Keys._
import Path.relativeTo
import com.twitter.scrooge.backend.{ServiceOption, WithFinagle}
import java.io.File
import scala.collection.immutable

object ScroogeSBT extends AutoPlugin {

  private[this] def generatedExtensionPattern(language: String): String =
    if (language.endsWith("java")) "*.java" else "*.scala"

  private[this] def compile(
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    language: String,
    flags: Set[ServiceOption],
    disableStrict: Boolean,
    scalaWarnOnJavaNSFallback: Boolean,
    defaultNamespace: String,
    addRootDirImporter: Boolean
  ): Unit = {

    val originalLoader: Option[ClassLoader] =
      // We have to change the class loader so Mustache resolver can see resources.
      // TODO: Please, remove it once Mustache is updated to >=0.9.
      if (language.toLowerCase == "java") {
        val original = Thread.currentThread.getContextClassLoader
        Thread.currentThread.setContextClassLoader(getClass.getClassLoader)
        Some(original)
      } else None

    val cfg = ScroogeConfig(
      destFolder = outputDir.getPath,
      includePaths = thriftIncludes.map(_.getPath).toList,
      thriftFiles = thriftFiles.map(_.getPath).toList,
      flags = flags,
      namespaceMappings = namespaceMappings,
      strict = !disableStrict,
      scalaWarnOnJavaNSFallback = scalaWarnOnJavaNSFallback,
      defaultNamespace = defaultNamespace,
      language = language.toLowerCase,
      addRootDirImporter = addRootDirImporter
    )

    try {
      val compiler = new Compiler(cfg)
      compiler.run()
    } finally {
      originalLoader.foreach(Thread.currentThread.setContextClassLoader)
    }
  }

  def filter(dependencies: Classpath, whitelist: Set[String]): Classpath = {
    dependencies.filter { dep =>
      // NOTE: module-id supports SBT pre-1.x while moduleID supports SBT 1.0 and later.
      val module = dep
        .get(AttributeKey[ModuleID]("module-id")).orElse(
          dep.get(AttributeKey[ModuleID]("moduleID")))
      module.exists { m => whitelist.contains(m.name) }
    }
  }

  object autoImport {
    val scroogeBuildOptions = SettingKey[Seq[ServiceOption]](
      "scrooge-build-options",
      "command line args to pass to scrooge"
    )

    val scroogePublishThrift = SettingKey[Boolean](
      "scrooge-publish-thrift",
      "Whether or not to publish thrift files in the jar"
    )

    val scroogeDisableStrict = SettingKey[Boolean](
      "scrooge-disable-strict",
      "issue warnings on non-severe parse errors instead of aborting"
    )

    val scroogeScalaWarnOnJavaNSFallback = SettingKey[Boolean](
      "scrooge-scala-warn-on-java-fallback",
      "Print a warning when the scala generator falls back to the java namespace"
    )

    val scroogeDefaultJavaNamespace = SettingKey[String](
      "scrooge-default-java-namespace",
      "Use <name> as default namespace if the thrift file doesn't define its own namespace. If this option is not specified either, then use \"thrift\" as default namespace"
    )

    val scroogeThriftDependencies = SettingKey[Seq[String]](
      "scrooge-thrift-dependencies",
      "artifacts to extract and compile thrift files from"
    )

    val scroogeThriftIncludeFolders = SettingKey[Seq[File]](
      "scrooge-thrift-include-folders",
      "folders to search for thrift 'include' directives"
    )

    val scroogeThriftIncludes = TaskKey[Seq[File]](
      "scrooge-thrift-includes",
      "complete list of folders to search for thrift 'include' directives"
    )

    val scroogeThriftIncludeRoot = SettingKey[Boolean](
      "scrooge-thrift-include-root",
      "If true scrooge will always search the project root for script files"
    )

    val scroogeThriftNamespaceMap = SettingKey[Map[String, String]](
      "scrooge-thrift-namespace-map",
      "namespace rewriting, to support generation of java/finagle/scrooge into the same jar"
    )

    val scroogeThriftSourceFolder = SettingKey[File](
      "scrooge-thrift-source-folder",
      "directory containing thrift source files"
    )

    val scroogeThriftExternalSourceFolder = SettingKey[File](
      "scrooge-thrift-external-source-folder",
      "directory containing external source files to compile"
    )

    val scroogeThriftSources = TaskKey[Seq[File]](
      "scrooge-thrift-sources",
      "complete list of thrift source files to compile"
    )

    val scroogeThriftOutputFolder = SettingKey[File](
      "scrooge-thrift-output-folder",
      "output folder for generated files (defaults to sourceManaged/thrift)"
    )

    val scroogeIsDirty = TaskKey[Boolean](
      "scrooge-is-dirty",
      "true if scrooge has decided it needs to regenerate the scala/java files from thrift sources"
    )

    val scroogeUnpackDeps = TaskKey[Seq[File]](
      "scrooge-unpack-deps",
      "unpack thrift files from dependencies, generating a sequence of source directories"
    )

    val scroogeGen = TaskKey[Seq[File]](
      "scrooge-gen",
      "generate code from thrift files using scrooge"
    )

    val scroogeLanguages = SettingKey[Seq[String]](
      "scrooge-languages",
      "language(s) to generate code in: scala, java, cocoa, android, lua"
    )
  }

  import autoImport._

  // only run if we are in the Jvm
  override def requires = sbt.plugins.JvmPlugin

  // we have no deps
  override def trigger = allRequirements

  // Dependencies included in the `thrift` configuration will be used
  // in both compile and test.
  val ThriftConfig = config("thrift")

  /**
   * these settings will go into both the compile and test configurations.
   * you can add them to other configurations by using inConfig(<config>)(genThriftSettings),
   * e.g. inConfig(Assembly)(genThriftSettings)
   */
  val genThriftSettings: Seq[Setting[_]] = Seq(
    scroogeBuildOptions := Seq(WithFinagle),
    scroogePublishThrift := false,
    scroogeDisableStrict := false,
    scroogeScalaWarnOnJavaNSFallback := false,
    scroogeDefaultJavaNamespace := "thrift",
    scroogeThriftSourceFolder := sourceDirectory.value / "thrift",
    scroogeThriftExternalSourceFolder := target.value / "thrift_external",
    scroogeThriftOutputFolder in Compile := (sourceManaged in Compile).value / "thrift",
    scroogeThriftOutputFolder in Test := (sourceManaged in Test).value / "thrift",
    scroogeThriftIncludeFolders := Seq(scroogeThriftSourceFolder.value),
    scroogeThriftNamespaceMap := Map(),
    scroogeThriftDependencies := Seq(),
    scroogeLanguages := Seq("scala"),
    libraryDependencies += "com.twitter" %% "scrooge-core" % com.twitter.BuildInfo.version,
    // complete list of source files
    scroogeThriftSources := (scroogeThriftSourceFolder.value ** "*.thrift").get,
    // complete list of include directories
    scroogeThriftIncludes := scroogeThriftIncludeFolders.value ++ scroogeUnpackDeps.value,
    scroogeThriftIncludeRoot := true,
    // unpack thrift files from all dependencies in the `thrift` configuration
    //
    // returns Seq[File] - directories that include thrift files
    scroogeUnpackDeps := {
      IO.createDirectory(scroogeThriftExternalSourceFolder.value)
      val whitelist = scroogeThriftDependencies.value.toSet
      val dependencies =
        Classpaths.managedJars(ThriftConfig, classpathTypes.value, update.value) ++
          filter(
            Classpaths.managedJars(configuration.value, classpathTypes.value, update.value),
            whitelist)

      val sourceFolder = scroogeThriftExternalSourceFolder.value
      val paths = dependencies.map {
        dep =>
          // NOTE: module-id supports SBT pre-1.x while moduleID supports SBT 1.0 and later.
          val module = dep
            .get(AttributeKey[ModuleID]("module-id")).orElse(
              dep.get(AttributeKey[ModuleID]("moduleID")))
          module.flatMap { m =>
            val dest = new File(sourceFolder, m.name)
            IO.createDirectory(dest)
            val thriftFiles = IO.unzip(dep.data, dest, "*.thrift").toSeq
            if (thriftFiles.isEmpty) {
              None
            } else {
              Some(dest)
            }
          }
      }
      paths.filter(_.isDefined).map(_.get)
    },
    // look at includes and our sources to see if anything is newer than any of our output files
    scroogeIsDirty := {
      // figure out if we need to actually rebuild, based on mtimes.
      val allSourceDeps: Seq[File] =
        scroogeThriftSources.value ++ scroogeThriftIncludes.value.foldLeft(Seq[File]()) {
          (files, dir) => files ++ (dir ** "*.thrift").get
        }
      val sourcesLastModified: Seq[Long] = allSourceDeps.map(_.lastModified)
      val newestSource: Long =
        if (sourcesLastModified.nonEmpty) {
          sourcesLastModified.max
        } else {
          Long.MaxValue
        }
      val outputsLastModified: Seq[Long] = {
        val outputFolder = scroogeThriftOutputFolder.value
        scroogeLanguages.value.flatMap { language =>
          (outputFolder ** generatedExtensionPattern(language)).get.map(_.lastModified)
        }
      }
      val oldestOutput: Long =
        if (outputsLastModified.nonEmpty) {
          outputsLastModified.min
        } else {
          Long.MinValue
        }
      oldestOutput < newestSource
    },
    // actually run scrooge
    scroogeGen := {
      val streamValue = streams.value
      val thriftSources = scroogeThriftSources.value
      val scroogeLangs = scroogeLanguages.value
      val outputFolder = scroogeThriftOutputFolder.value
      val thriftIncludes = scroogeThriftIncludes.value
      val namespaceMap = scroogeThriftNamespaceMap.value
      val buildOptions = scroogeBuildOptions.value
      val disableStrict = scroogeDisableStrict.value
      val warnOnFallBack = scroogeScalaWarnOnJavaNSFallback.value
      val javaNamespace = scroogeDefaultJavaNamespace.value
      val addRootDirImporter = scroogeThriftIncludeRoot.value
      // for some reason, sbt sometimes calls us multiple times, often with no source files.
      if (scroogeIsDirty.value && thriftSources.nonEmpty) {
        streamValue.log.info(s"Generating scrooge thrift for ${thriftSources.mkString(", ")} ...")
        scroogeLangs.foreach { language =>
          compile(
            outputFolder,
            thriftSources.toSet,
            thriftIncludes.toSet,
            namespaceMap,
            language,
            buildOptions.toSet,
            disableStrict,
            warnOnFallBack,
            javaNamespace,
            addRootDirImporter
          )
        }
      }
      scroogeLanguages.value.flatMap { language =>
        (outputFolder ** generatedExtensionPattern(language)).get
      }
    },
    sourceGenerators += scroogeGen
  )

  val packageThrift = mappings in (Compile, packageBin) ++= {
    val thriftSources = (scroogeThriftSources in Compile).value
    val nameValue = name.value
    (if ((scroogePublishThrift in Compile).value) thriftSources else Nil).map { file =>
      file -> s"$nameValue/${file.name}"
    }
  }

  private[this] val generatedSources = mappings in (Compile, packageSrc) ++= {
    val thriftOutputFolder = (scroogeThriftOutputFolder in Compile).value
    ((thriftOutputFolder ** "*") filter { _.isFile }).get pair relativeTo(thriftOutputFolder)
  }

  override lazy val projectSettings =
    Seq(ivyConfigurations += ThriftConfig) ++
      inConfig(Test)(genThriftSettings) ++
      inConfig(Compile)(genThriftSettings) :+ packageThrift :+ generatedSources

  @deprecated("Settings auto-imported via AutoPlugin mechanism", "2015-03-24")
  lazy val newSettings = projectSettings
}
