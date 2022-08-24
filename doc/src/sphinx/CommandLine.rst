Command Line
============

Aside from integrating with SBT and maven, Scrooge can also be run directly
from the command line. One way to do so is to run the main files of the
'scrooge-generator' or 'scrooge-linter' projects with the help of SBT. This
requires having the Scrooge project available locally.

To start, clone the Scrooge repository from GitHub, change into
the Scrooge directory, and checkout the master branch:

.. code-block:: bash

    $ git clone https://github.com/twitter/scrooge.git
    $ cd scrooge
    $ git checkout master

Working off of the master branch will ensure that you're using the latest
released version of Scrooge.

scrooge-generator
~~~~~~~~~~~~~~~~~

The 'scrooge-generator' project is responsible for converting Thrift IDL
files into generated code. Running it from the command line requires
running the 'com.twitter.scrooge.Main' object.

To get command line help:

.. code-block:: bash

    $ ./sbt 'scrooge-generator/runMain com.twitter.scrooge.Main --help'

To generate source with content written to the current directory:

.. code-block:: bash

    $ ./sbt 'scrooge-generator/runMain com.twitter.scrooge.Main <thrift-file1> [<thrift-file2> ...]'

To generate source with content written to a specified directory, using
extra include paths, rebuilding only those files that have changed:

.. code-block:: bash

    $ ./sbt 'scrooge-generator/runMain com.twitter.scrooge.Main -d <target-dir> -i <include-path> -s <thrift-file1> [<thrift-file2> ...]'

As an example, to generate Scala code for the 'user.thrift' file from the
'scrooge-maven-demo', you can run the following command:

.. code-block:: bash

    $ ./sbt 'scrooge-generator/runMain com.twitter.scrooge.Main -d generated -i demos/scrooge-maven-demo/src/main/thrift -s user.thrift'

This will output the 'User.scala' and 'UserService.scala' files in namespaced
directories underneath the 'generated' directory.

A complete command line help menu for scrooge-generator:

.. code-block:: none

  Usage: scrooge [options] <files...>

  --help                                    show this help screen
  -V, --version                             print version and quit
  -v, --verbose                             log verbose messages about progress
  -d, --dest <path>                         write generated code to a folder (default: .)
  -i, --include-path <path>                 path(s) to search for included thrift files (may be used multiple times)
  -n, --namespace-map <oldname>=<newname>   map old namespace to new (may be used multiple times)
  --default-java-namespace <name>           Use <name> as default namespace if the thrift file doesn't define its own namespace. If this option is not specified either, then use "thrift" as default namespace
  --disable-strict                          issue warnings on non-severe parse errors instead of aborting
  --gen-file-map <path>                     generate map.txt in the destination folder to specify the mapping from input thrift files to output Scala/Java files
  --dry-run                                 parses and validates source thrift files, reporting any errors, but does not emit any generated source code.  can be used with --gen-file-mapping to get the file mapping
  -s, --skip-unchanged                      Don't re-generate if the target is newer than the input
  -l, --language <value>                    name of language to generate code in (currently supported languages: java, lua, scala, cocoa, android, swift)
  --java-ser-enum-type                      Encode a thrift enum as o.a.t.p.TType.ENUM instead of TType.I32
  --language-flag <flag>                    Pass arguments to supported language generators. To generate Scala 2.13 compatible `scala.Seq` alias as `scala.collection.immutable.Seq`, please use "immutable-sequences".
  --scala-warn-on-java-ns-fallback          Print a warning when the scala generator falls back to the java namespace
  --finagle                                 generate finagle classes
  --gen-adapt                               Generate code for adaptive decoding for scala.
  --java-passthrough                        Enable java passthrough
  <files...>                                thrift files to compile

scrooge-linter
~~~~~~~~~~~~~~

The 'scrooge-linter' project is responsible for verifying that a Thrift
IDL file complies with Scrooge's understanding of the Thrift grammar. It
can be useful when making modifications to Thrift files to check it via
the 'scrooge-linter' without running the full on 'scrooge-generator'.

To get command line help:

.. code-block:: bash

    $ ./sbt 'scrooge-linter/runMain com.twitter.scrooge.linter.Main --help'

To lint a specific file (or files):

.. code-block:: bash

    $ ./sbt 'scrooge-linter/runMain com.twitter.scrooge.linter.Main <files...>'

As an example, to lint the 'user.thrift' file from the 'scrooge-maven-demo', you
can run the following command:

.. code-block:: bash

    $ ./sbt 'scrooge-linter/runMain com.twitter.scrooge.linter.Main -n demos/scrooge-maven-demo/src/main/thrift user.thrift'

A complete command line help menu for scrooge-linter:

.. code-block:: none

  Usage: scrooge-linter [options] <files...>

  --help                                 show this help screen
  -V, --version                          print version and quit
  -v, --verbose                          log verbose messages about progress
  -i, --ignore-errors                    return 0 if linter errors are found. If not set, linter returns 1.
  -n, --include-path <path>              path(s) to search for included thrift files (may be used multiple times)
  -e, --enable-rule <rule-name>          rules to be enabled.
    Available: Namespaces, CompilerOptimizedMethodParamLimit, RelativeIncludes, CamelCase, RequiredFieldDefault, Keywords, TransitivePersistence, FieldIndexGreaterThanZeroRule, MalformedDocstring, MapKeyType, DocumentedPersisted
      Default: Namespaces, CompilerOptimizedMethodParamLimit, RelativeIncludes, CamelCase, RequiredFieldDefault, Keywords, TransitivePersistence, FieldIndexGreaterThanZeroRule, MalformedDocstring, MapKeyType
  -d, --disable-rule <rule-name>         rules to be disabled.
  -p, --ignore-parse-errors              continue if parsing errors are found.
  -w, --warnings                         show linter warnings (default = False)
  --disable-strict                       issue warnings on non-severe parse errors instead of aborting
  --fatal-warnings                       convert warnings to errors
  <files...>                             thrift files to compile
