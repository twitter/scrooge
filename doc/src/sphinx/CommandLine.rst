Command Line
============

To get command line help:

::

    $ scrooge --help

To generate source with content written to the current directory:

::

    $ scrooge <thrift-file1> [<thrift-file2> ...]

To generate source with content written to a specified directory, using
extra include paths, rebuilding only those files that have changed:

::

    $ scrooge
      -d <target-dir>
      -i <include-path>
      -s
      <thrift-file1> [<thrift-file2> ...]

A complete command line help menu:

::

    Usage: scrooge [options] <files...>

      --help
            show this help screen
      -V | --version
            print version and quit
      -v | --verbose
            log verbose messages about progress
      -d <path> | --dest <path>
            write generated code to a folder (default: .)
      -i <path> | --import-path <path>
            path(s) to search for imported thrift files (may be used multiple times)
      -n <oldname>=<newname> | --namespace-map <oldname>=<newname>
            map old namespace to new (may be used multiple times)
      --disable-strict
            issue warnings on non-severe parse errors instead of aborting
      -s | --skip-unchanged
            Don't re-generate if the target is newer than the input
      -l <value> | --language <value>
            name of language to generate
      --finagle
            generate finagle classes
      <files...>
            thrift files to compile
