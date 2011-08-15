# scrooge

Scrooge is a thrift code generator for scala.

Since scala is API-compatible with java, you can use the apache thrift code
generator to generate java files and use them from within scala, but the
overhead of converting to/from java containers can sometimes be annoying.
This is an attempt to bypass the problem by generating scala code directly.
It also uses scala syntax so the generated code is much smaller.

The generated code still depends on libthrift.


## Building

To build scrooge, use sbt:

    $ sbt package-dist


## Work in progress

This is still a work in progress, so patches and help are gleefully accepted.

The state (as of July 2011):

- The code generator is basically complete, and has been used to generate the
  thrift interface for a test server. You can find that server here:

      git clone http://code.lag.net/sqrt2

  The "master" branch uses sbt-thrift (apache thrift with scala wrappers) and
  the "scrooge" branch uses scrooge.

- There's no sbt plugin yet, but there should be.

## Running Scrooge

This assumes that you have a script named "scrooge" in your path.

- To get command line help:

    `$ ./scrooge -?`
    
- To generate source with content written to the current directory:

    `$ ./scrooge <thrift-file1> [<thrift-file2> ...]`
    
- To generate source with content written to a specified directory:

    `$ ./scrooge -d <target-dir> <thrift-file1> [<thrift-file2> ...]`
    
- To specify additional include paths:

    `$ ./scrooge -i <include-path> <thrift-file1> [<thrift-file2> ...]`
    
    Where include-path is a list of directory, separated by the platform specified path separator (':' on unix, ';' on windows)
