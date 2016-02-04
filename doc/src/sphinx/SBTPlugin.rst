SBT Plugin
==========

Add the following lines to your `project/plugins.sbt` file:

::

    resolvers += "twitter-repo" at "https://maven.twttr.com"

    addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "4.5.0")

In your `build.sbt` file:

::

    libraryDependencies ++= Seq(
      "org.apache.thrift" % "libthrift" % "0.8.0",
      "com.twitter" %% "scrooge-core" % "4.5.0",
      "com.twitter" %% "finagle-thrift" % "6.33.0"
    )

or, in your `project/Build.scala` file:

::

    lazy val myProject = Project(
      id = "my-project",
      base = file("my-project"),
      settings = Project.defaultSettings
    ).settings(
      libraryDependencies ++= Seq(
        "org.apache.thrift" % "libthrift" % "0.8.0",
        "com.twitter" %% "scrooge-core" % "4.5.0",
        "com.twitter" %% "finagle-thrift" % "6.33.0"
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


Migrating from 3.x.x to 3.18.0
------------------------------

SBT 0.13.5 and above
~~~~~~~~~~~~~~~~~~~~

The Scrooge SBT plugin now uses SBT's `auto plugin system
<http://www.scala-sbt.org/0.13/docs/Plugins.html>`_, which is only available
for SBT 0.13.5 and later.

Projects using  `build.sbt`
~~~~~~~~~~~~~~~~~~~~~~~~~~~

For 3.17.0 and earlier releases of the plugin, it was necessary to add the
settings to your build configuration explicitly:

::

    import com.twitter.scrooge.ScroogeSBT

    lazy val app = project.in(file("app"))
      .settings(ScroogeSBT.newSettings: _*)
      .settings(
        // more settings here
      )

This is no longer necessary, so the build above should look like this:

::

    lazy val app = project.in(file("app"))
      .settings(
        // more settings here
      )


The default settings are included automatically, by virtue of the auto plugin
mechanism.

The plugin's setting and task keys are also now brought into scope
automatically, so the following:

::

    import com.twitter.scrooge.ScroogeSBT

    lazy val app = project.in(file("app"))
      .settings(ScroogeSBT.newSettings: _*)
      .settings(
        ScroogeSBT.scroogeThriftSourceFolder in Compile <<= baseDirectory {
          base => base / "src/main/resources"
        }
      )

becomes simply:

::

    lazy val app = project.in(file("app"))
      .settings(
        scroogeThriftSourceFolder in Compile <<= baseDirectory {
          base => base / "src/main/resources"
        }
      )


Using `Build.scala`
~~~~~~~~~~~~~~~~~~~

The big change here, in addition to the automatically injected settings, is
that the location of the keys has changed. They are now under an object called
`autoImport`. So this:

::

    import sbt._
    import Keys._

    import com.twitter.scrooge.ScroogeSBT._

    object build extends Build {
      lazy val app = Project(
        id = "app",
        settings = Project.defaultSettings ++ newSettings
      ).settings(
        scroogeThriftSourceFolder in Compile <<= baseDirectory {
          base => base / "src/main/resources"
        }
      )
    }

becomes:

::

    import sbt._
    import Keys._

    import com.twitter.scrooge.ScroogeSBT.autoImport._

    object build extends Build {

      lazy val app = Project(
        id = "app",
        base = file("app"),
        settings = Project.defaultSettings
      ).settings(
        scroogeThriftSourceFolder in Compile <<= baseDirectory {
          base => base / "src/main/resources"
        }
      )
    }

That is to say: adjust the imports, and drop the settings injection.
