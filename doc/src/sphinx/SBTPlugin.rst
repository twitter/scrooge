SBT Plugin
==========

Scrooge is capable of integrating with the Scala Build Tool (SBT). It works
with either version 1.x or the older 0.13. It does so through an SBT plugin,
aptly named 'scrooge-sbt-plugin'.

Plugin Addition
~~~~~~~~~~~~~~~

To use the scrooge-sbt-plugin, add the following lines to your
`project/plugins.sbt` file:

::

    addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "23.11.0")

Incorporating this line makes the Scrooge plugin available for use by SBT.
Thrift files added to your project can now have code generated for them. By
default the generated code will be Scala code intended for use with Finagle.
It is common to place Thrift files in a namespaced directory under
'src/main/thrift'. This location is configurable via the
`scroogeThriftSourceFolder` plugin setting, and other common plugin settings are
listed below.

Code Dependency Additions
~~~~~~~~~~~~~~~~~~~~~~~~~

Generated code will now be available to your project, however the generated
code won't compile without adding a few additional dependencies to your
project. These should be added to your project's `libraryDependencies` in
either your `build.sbt` file or your `project/Build.scala` file, depending on
how your project is set up.

An example using a `build.sbt` file.

::

    lazy val root = (project in file("."))
      .settings(
        name := "Scrooge Demo",
        libraryDependencies ++= Seq(
          "org.apache.thrift" % "libthrift" % "0.10.0",
          "com.twitter" %% "scrooge-core" % "23.11.0",
          "com.twitter" %% "finagle-thrift" % "23.11.0",
          scalaTest % Test
        )
      )

Scrooge SBT Configuration Options
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Scrooge SBT plugin provides a multitude of settings to determine
namespaces, dependencies, source file locations, output file locations,
what languages to generate code for, and whether that code should be
setup to use Finagle. A full list of the available settings is in the
ScroogeSBT.scala file. Here are the ones that you'll most likely want to edit:

- **scroogeLanguages: Seq[String]**

  list of languages for which to generate code
  (default: `Seq("scala")`)

- **scroogeBuildOptions: Seq[String]**

  list of command-line arguments to pass to scrooge
  (default: `Seq("WithFinagle")`)

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
  (default: `target/<scala-ver>/src_managed/main/thrift`)

Scrooge SBT plugin settings can be modified by changing the value of an
individual setting within the SBT project definition. Most likely you'll want to
modify the Scrooge setting for the `Compiler` phase.

An example of modifying a `build.sbt` file to change a project's settings to
have Scrooge generate Java code.

::

    lazy val root = (project in file("."))
      .settings(
        name := "Scrooge Demo",
        scroogeBuildOptions in Compile := Seq(),
        scroogeLanguages in Compile := Seq("java"),
        libraryDependencies ++= Seq(
          "org.apache.thrift" % "libthrift" % "0.10.0",
          "com.twitter" %% "scrooge-core" % "23.11.0",
          "com.twitter" %% "finagle-thrift" % "23.11.0",
          scalaTest % Test
        )
