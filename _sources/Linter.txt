Scrooge thrift linter
=====================

Scrooge thrift linter enforces usage guidelines for thrift files. These guidelines make sharing thrift files between projects easy.

Running the linter
------------------------

To run the thrift linter with sbt:
::

    $ ./sbt 'scrooge-linter/run-main com.twitter.scrooge.linter.Main --help'

    $ ./sbt 'scrooge-linter/run-main com.twitter.scrooge.linter.Main /path/to/thrift/file.thrift'

List of rules
-------------

The linter produces warnings and errors. By default, only the errors are shown. Use the '-w' flag to display linter warnings.

The error-level linter rules are:

1. Each thrift file must have a scala and a java namespace.

2. No relative includes ouside the current directory, i.e. no '..' in included paths.

   Absolute paths are preferred. Including files in the subdirectories is fine.

3. No default values for required fields.

The warning-level rules are:

1. Struct names must be UpperCamelCase.

   Field names must be lowerCamelCase.

2. Struct and field names must not be keywords in Scala, Java, Ruby, Python, PHP.
