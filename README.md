
## scrooge

Scrooge is a thrift code generator for scala.

Since scala is API-compatible with java, you can use the apache thrift code
generator to generate java files and use them from within scala, but the
overhead of converting to/from java containers can sometimes be annoying.
This is an attempt to bypass the problem by generating scala code directly.
It also uses scala syntax so the generated code is much smaller.

The generated code still depends on libthrift.


# Building

To build scrooge, use sbt:

    $ sbt package-dist


# Work in progress

This is still a work in progress, so patches and help are gleefully accepted.

The state (as of July 2011):

- The code generator is basically complete, and has been used to generate the
  thrift interface for a test server. You can find that server here:

      git clone http://code.lag.net/sqrt2)

- To run the code generator, just do:

      $ ./scrooge <thrift-file>

  and it will spew all the generated code to stdout. This is not intended to
  be the final behavior -- just a stopgap. It really should take some options
  and write out several files to a single path.

- There's no sbt plugin yet, but there should be.
