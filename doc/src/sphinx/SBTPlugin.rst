SBT Plugin
==========

Add a line like this to your `project/plugins.sbt` file:

::

    addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "3.3.2")

In your `build.sbt` file:

::

    com.twitter.scrooge.ScroogeSBT.newSettings

    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % "0.8.0",
      "com.twitter" %% "scrooge-core" % "3.3.2",
      "com.twitter" %% "finagle-thrift" % "6.5.0"
    )

or, in your `project/Build.scala` file:

::

    import com.twitter.scrooge.ScroogeSBT

    lazy val myProject = Project(
      id = "my-project",
      settings = Project.defaultSettings ++
        ScroogeSBT.newSettings
    ).settings(
      libraryDependencies ++= Seq(
        "org.apache.thrift" % "libthrift" % "0.8.0",
        "com.twitter" %% "scrooge-core" % "3.3.2",
        "com.twitter" %% "finagle-thrift" % "6.5.0"
      )
    )


**Configuration Options**

A full list of settings is in the (only) source file. Here are the ones you're
most likely to want to edit:

- **scroogeBuildOptions: Seq[String]**

  list of command-line arguments to pass to scrooge
  (default: `Seq("--finagle", "--verbose")`)

- **scroogeThriftDependencies: Seq[String]**

  artifacts to extract and compile thrift files from

- **scroogeThriftIncludeFolders: Seq[File]**

  list of folders to search when processing "include" directives
  (default: none)

- **scroogeThriftSourceFolder: File**

  where to find thrift files to compile
  (default: `src/main/thrift/`)

- **scroogeThriftOutputFolder: File**

  where to put the generated scala files
  (default: `target/<scala-ver>/src_managed`)
