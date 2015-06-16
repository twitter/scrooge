package com.twitter.scrooge

import sbt._
import Keys._

import java.io.File

object ScroogeSBT extends AutoPlugin {

  private[this] def generatedExtensionPattern(language: String): String =
    if (language.endsWith("java")) "*.java" else "*.scala"

  def compile(
    log: Logger,
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    language: String,
    flags: Set[String]
  ) {

    val originalLoader: Option[ClassLoader] =
      // We have to change the class loader so Mustache resolver can see resources.
      // TODO: Please, remove it once Mustache is updated to >=0.9.
      if (language.toLowerCase == "java") {
        val original = Thread.currentThread.getContextClassLoader
        Thread.currentThread.setContextClassLoader(getClass.getClassLoader)
        Some(original)
      } else None

    try {
      val compiler = new Compiler()
      compiler.destFolder = outputDir.getPath
      thriftIncludes.map { compiler.includePaths += _.getPath }
      namespaceMappings.map { e => compiler.namespaceMappings.put(e._1, e._2) }
      Main.parseOptions(compiler, flags.toSeq ++ thriftFiles.map { _.getPath })
      compiler.language = language.toLowerCase
      compiler.run()
    } finally {
      originalLoader.foreach(Thread.currentThread.setContextClassLoader)
    }
  }

  def filter(dependencies: Classpath, whitelist: Set[String]): Classpath = {
    dependencies.filter { dep =>
      val module = dep.get(AttributeKey[ModuleID]("module-id"))
      module.exists { m =>
        whitelist.contains(m.name)
      }
    }
  }

  object autoImport {

    val scroogeBuildOptions = SettingKey[Seq[String]](
      "scrooge-build-options",
      "command line args to pass to scrooge"
    )

    val scroogePublishThrift = SettingKey[Boolean](
      "scrooge-publish-thrift",
      "Whether or not to publish thrift files in the jar"
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
      "output folder for generated files (defaults to sourceManaged)"
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

    val scroogeLanguage = SettingKey[String](
      "scrooge-language",
      "language to generate code in: scala, java, experimental-java"
    )
  }

  import autoImport._

  // only run if we are in the Jvm
  override def requires = sbt.plugins.JvmPlugin

  // we have no deps
  override def trigger = allRequirements

  // Dependencies included in the `thrift` configuration will be used
  // in both compile and test.
  val thriftConfig = config("thrift")

  /**
   * these settings will go into both the compile and test configurations.
   * you can add them to other configurations by using inConfig(<config>)(genThriftSettings),
   * e.g. inConfig(Assembly)(genThriftSettings)
   */
  val genThriftSettings: Seq[Setting[_]] = Seq(
    scroogeBuildOptions := Seq("--finagle"),
    scroogePublishThrift := false,
    scroogeThriftSourceFolder <<= (sourceDirectory) { _ / "thrift" },
    scroogeThriftExternalSourceFolder <<= (target) { _ / "thrift_external" },
    scroogeThriftOutputFolder <<= (sourceManaged) { identity },
    scroogeThriftIncludeFolders <<= (scroogeThriftSourceFolder) { Seq(_) },
    scroogeThriftNamespaceMap := Map(),
    scroogeThriftDependencies := Seq(),
    scroogeLanguage := "scala",
    libraryDependencies += "com.twitter" %% "scrooge-core" % com.twitter.BuildInfo.version,

    // complete list of source files
    scroogeThriftSources <<= scroogeThriftSourceFolder map { (srcDir) => (srcDir ** "*.thrift").get },

    // complete list of include directories
    scroogeThriftIncludes <<= (
      scroogeThriftIncludeFolders,
      scroogeUnpackDeps
    ) map { (includes, extDirs) =>
      includes ++ extDirs
    },

    // unpack thrift files from all dependencies in the `thrift` configuration
    //
    // returns Seq[File] - directories that include thrift files
    scroogeUnpackDeps <<= (
      streams,
      configuration,
      classpathTypes,
      update,
      scroogeThriftDependencies,
      scroogeThriftExternalSourceFolder
    ) map { (out, configuration, cpTypes, update, deps, extDir) =>
      IO.createDirectory(extDir)
      val whitelist = deps.toSet
      val dependencies =
        Classpaths.managedJars(thriftConfig, cpTypes, update) ++
        filter(Classpaths.managedJars(configuration, cpTypes, update), whitelist)

      val paths = dependencies.map { dep =>
        val module = dep.get(AttributeKey[ModuleID]("module-id"))
        module.flatMap { m =>
          val dest = new File(extDir, m.name)
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
    scroogeIsDirty <<= (
      streams,
      scroogeThriftSources,
      scroogeThriftOutputFolder,
      scroogeThriftIncludes,
      scroogeLanguage
    ) map { (out, sources, outputDir, inc, language) =>
      // figure out if we need to actually rebuild, based on mtimes.
      val allSourceDeps = sources ++ inc.foldLeft(Seq[File]()) { (files, dir) =>
        files ++ (dir ** "*.thrift").get
      }
      val sourcesLastModified: Seq[Long] = allSourceDeps.map(_.lastModified)
      val newestSource = if (sourcesLastModified.size > 0) {
        sourcesLastModified.max
      } else {
        Long.MaxValue
      }
      val outputsLastModified = (outputDir ** generatedExtensionPattern(language)).get.map(_.lastModified)
      val oldestOutput = if (outputsLastModified.size > 0) {
        outputsLastModified.min
      } else {
        Long.MinValue
      }
      oldestOutput < newestSource
    },

    // actually run scrooge
    scroogeGen <<= (
      streams,
      scroogeIsDirty,
      scroogeThriftSources,
      scroogeThriftOutputFolder,
      scroogeBuildOptions,
      scroogeThriftIncludes,
      scroogeThriftNamespaceMap,
      scroogeLanguage
    ) map { (out, isDirty, sources, outputDir, opts, inc, ns, language) =>
      // for some reason, sbt sometimes calls us multiple times, often with no source files.
      if (isDirty && sources.nonEmpty) {
        out.log.info("Generating scrooge thrift for %s ...".format(sources.mkString(", ")))
        compile(out.log, outputDir, sources.toSet, inc.toSet, ns, language, opts.toSet)
      }
      (outputDir ** generatedExtensionPattern(language)).get.toSeq
    },
    sourceGenerators <+= scroogeGen
  )

  val packageThrift = mappings in (Compile, packageBin) ++= {
    (if ((scroogePublishThrift in Compile).value) (scroogeThriftSources in Compile).value else Nil).map { file =>
      file -> s"${name.value}/${file.name}"
    }
  }

  override lazy val projectSettings =
    Seq(ivyConfigurations += thriftConfig) ++
    inConfig(Test)(genThriftSettings) ++
    inConfig(Compile)(genThriftSettings) :+ packageThrift

  @deprecated("Settings auto-imported via AutoPlugin mechanism", "2015-03-24")
  lazy val newSettings = projectSettings
}
