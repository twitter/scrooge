package com.twitter.scrooge

import sbt._
import Keys._

import java.io.File
import com.twitter.scrooge.backend.{WithFinagle, ServiceOption}

object ScroogeSBT extends Plugin {

  def compile(
    log: Logger,
    outputDir: File,
    thriftFiles: Set[File],
    thriftIncludes: Set[File],
    namespaceMappings: Map[String, String],
    language: String,
    flags: Set[ServiceOption],
    disableStrict: Boolean,
    scalaWarnOnJavaNSFallback: Boolean,
    experimentalFlags: Set[String],
    defaultNamespace: String
  ) {
    val compiler = new Compiler()
    compiler.destFolder = outputDir.getPath
    thriftIncludes.map { compiler.includePaths += _.getPath }
    namespaceMappings.map { e => compiler.namespaceMappings.put(e._1, e._2)}
    compiler.flags ++= flags
    compiler.strict = !disableStrict
    compiler.scalaWarnOnJavaNSFallback = scalaWarnOnJavaNSFallback
    compiler.experimentFlags ++= experimentalFlags
    compiler.defaultNamespace = defaultNamespace
    compiler.thriftFiles ++= thriftFiles.map(_.getPath())
    compiler.language = language.toLowerCase
    compiler.run()
  }

  def filter(dependencies: Classpath, whitelist: Set[String]): Classpath = {
    dependencies.filter { dep =>
      val module = dep.get(AttributeKey[ModuleID]("module-id"))
      module.map { m =>
        whitelist.contains(m.name)
      }.getOrElse(false)
    }
  }

  val scroogeBuildOptions = SettingKey[Seq[ServiceOption]](
    "scrooge-build-options",
    "command line args to pass to scrooge"
  )

  val scroogeDisableStrict = SettingKey[Boolean](
    "scrooge-disable-strict",
    "issue warnings on non-severe parse errors instead of aborting"
  )

  val scroogeScalaWarnOnJavaNSFallback = SettingKey[Boolean](
    "scrooge-scala-warn-on-java-fallback",
    "Print a warning when the scala generator falls back to the java namespace"
  )

  val scroogeExperimentalFlags = SettingKey[Set[String]](
    "scrooge-experimental-flags",
    "[EXPERIMENTAL] DO NOT USE FOR PRODUCTION. This is meant only for enabling/disabling features for benchmarking"
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
    "output folder for generated scala files (defaults to sourceManaged)"
  )

  val scroogeIsDirty = TaskKey[Boolean](
    "scrooge-is-dirty",
    "true if scrooge has decided it needs to regenerate the scala files from thrift sources"
  )

  val scroogeUnpackDeps = TaskKey[Seq[File]](
    "scrooge-unpack-deps",
    "unpack thrift files from dependencies, generating a sequence of source directories"
  )

  val scroogeGen = TaskKey[Seq[File]](
    "scrooge-gen",
    "generate scala code from thrift files using scrooge"
  )

  // Dependencies included in the `thrift` configuration will be used
  // in both compile and test.
  val thriftConfig = config("thrift")

  /**
   * these settings will go into both the compile and test configurations.
   * you can add them to other configurations by using inConfig(<config>)(genThriftSettings),
   * e.g. inConfig(Assembly)(genThriftSettings)
   */
  val genThriftSettings: Seq[Setting[_]] = Seq(
    scroogeBuildOptions := Seq(WithFinagle),
    scroogeDisableStrict := false,
    scroogeScalaWarnOnJavaNSFallback := false,
    scroogeDefaultJavaNamespace := "thrift",
    scroogeExperimentalFlags := Set(),
    scroogeThriftSourceFolder <<= (sourceDirectory) { _ / "thrift" },
    scroogeThriftExternalSourceFolder <<= (target) { _ / "thrift_external" },
    scroogeThriftOutputFolder <<= (sourceManaged) { identity },
    scroogeThriftIncludeFolders <<= (scroogeThriftSourceFolder) { Seq(_) },
    scroogeThriftNamespaceMap := Map(),
    scroogeThriftDependencies := Seq(),

    // complete list of source files
    scroogeThriftSources <<= (
      scroogeThriftSourceFolder,
      scroogeUnpackDeps
    ) map { (srcDir, extDirs) =>
      (Seq(srcDir) ++ extDirs).flatMap { dir => (dir ** "*.thrift").get }
    },

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
      scroogeThriftIncludes
    ) map { (out, sources, outputDir, inc) =>
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
      val outputsLastModified = (outputDir ** "*.scala").get.map(_.lastModified)
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
      scroogeDisableStrict,
      scroogeScalaWarnOnJavaNSFallback,
      scroogeExperimentalFlags,
      scroogeDefaultJavaNamespace
    ) map { (out, isDirty, sources, outputDir, opts, inc, ns, disableStrict, warnOnJavaNSFallback, exFlags, defaultNamespace) =>
      // for some reason, sbt sometimes calls us multiple times, often with no source files.
      if (isDirty && !sources.isEmpty) {
        out.log.info("Generating scrooge thrift for %s ...".format(sources.mkString(", ")))
        compile(out.log, outputDir, sources.toSet, inc.toSet, ns, "scala", opts.toSet, disableStrict, warnOnJavaNSFallback, exFlags, defaultNamespace)
      }
      (outputDir ** "*.scala").get.toSeq
    },
    sourceGenerators <+= scroogeGen
  )

  val newSettings =
    Seq(ivyConfigurations += thriftConfig) ++
    inConfig(Test)(genThriftSettings) ++
    inConfig(Compile)(genThriftSettings)
}
