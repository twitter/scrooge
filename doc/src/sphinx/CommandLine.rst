Command Line
============

To get command line help:

::

    $ ./sbt 'scrooge-generator/run-main com.twitter.scrooge.Main --help

To generate source with content written to the current directory:

::

    $ ./sbt 'scrooge-generator/run-main com.twitter.scrooge.Main <thrift-file1> [<thrift-file2> ...]

To generate source with content written to a specified directory, using
extra include paths, rebuilding only those files that have changed:

::

    $ ./sbt 'scrooge-generator/run-main com.twitter.scrooge.Main \
      -d <target-dir>   \
      -i <include-path> \
      -s                \
      <thrift-file1> [<thrift-file2> ...]

A complete command line help menu:

::

    Usage: ./sbt 'scrooge-generator/run-main com.twitter.scrooge.Main [options] <files...>

      --help
            show this help screen
      -V | --version
            print version and quit
      -v | --verbose
            log verbose messages about progress
      -d <path> | --dest <path>
            write generated code to a folder (default: .)
      --import-path <path>
            [DEPRECATED] path(s) to search for included thrift files (may be used multiple times)
      -i <path> | --include-path <path>
            path(s) to search for included thrift files (may be used multiple times)
      -n <oldname>=<newname> | --namespace-map <oldname>=<newname>
            map old namespace to new (may be used multiple times)
      --default-java-namespace <name>
            Use <name> as default namespace if the thrift file doesn't define its own namespace. If this option is not specified either, then use "thrift" as default namespace
      --disable-strict
            issue warnings on non-severe parse errors instead of aborting
      --gen-file-map <path>
            generate map.txt in the destination folder to specify the mapping from input thrift files to output Scala/Java files
      --dry-run
            parses and validates source thrift files, reporting any errors, but does not emit any generated source code.  can be used with --gen-file-mapping to get the file mapping
      -s | --skip-unchanged
            Don't re-generate if the target is newer than the input
      -l <value> | --language <value>
            name of language to generate code in ('experimental-java' and 'scala' are currently supported)
      --experiment-flag <flag>
            [EXPERIMENTAL] DO NOT USE FOR PRODUCTION. This is meant only for enabling/disabling features for benchmarking
      --scala-warn-on-java-ns-fallback
            Print a warning when the scala generator falls back to the java namespace
      --finagle
            generate finagle classes
      <files...>
            thrift files to compile
