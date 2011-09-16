# scrooge

Scrooge is a thrift code generator for scala.

It's meant to be a drop-in replacement for the apache thrift code generator.

Since scala is API-compatible with java, you can use the apache thrift code
generator to generate java files and use them from within scala, but the
overhead of converting to/from java containers can sometimes be annoying.
This is an attempt to bypass the problem by generating scala code directly.
It also uses scala syntax so the generated code is *much* smaller.

The generated code still depends on libthrift.

There is a fairly comprehensive set of unit tests, which actually generate
code, compile it, and execute it to verify expectations.


## Building

To build scrooge, use sbt:

    $ sbt package-dist


## Features

- Generates native scala thrift codecs, using case classes and functions.

- Generated code is templated using a mustache variant, making it easy to
  edit.

- Finagle client/server adaptors, and Ostrich wrappers, can be optionally
  generated at the same time.


## Running Scrooge

A starter script is built into `dist/scrooge/scripts`. You can run that or
write your own.

To get command line help:

    $ scrooge --help

To generate source with content written to the current directory:

    $ scrooge <thrift-file1> [<thrift-file2> ...]

To generate source with content written to a specified directory, using
extra include paths, rebuilding only those files that have changed:

    $ scrooge
      -d <target-dir>
      -i <include-path>
      -s
      <thrift-file1> [<thrift-file2> ...]


## Runtime dependency

There are a couple of classes needed by the generated code. These have been
moved out of scrooge into a separate jar to keep dependencies small:

    val scrooge_runtime = "com.twitter" % "scrooge-runtime" % "1.0.2"


## SBT Plugin

There's a plugin for SBT (0.7.x) which is meant to be a drop-in replacement
for sbt-thrift:

[https://github.com/twitter/sbt-scrooge](https://github.com/twitter/sbt-scrooge)

To use it, replace the "sbt-thrift" line in your `Plugins.scala` file with:

    val sbtScrooge = "com.twitter" % "sbt-scrooge" % "1.1.1"

(or whatever the current version is) and add `with CompileThriftScroogeMixin`
or `with CompileThriftScrooge` to your project's mixin list. More details are
in the sbt-scrooge `README`.


## License

Scrooge is licensed under the Apache 2 license, which you can find in the
included file `LICENSE`.


## Credits / Thanks

- Jorge Ortiz
- Robey Pointer
- Ian Ownbey
- Jeremy Cloud
