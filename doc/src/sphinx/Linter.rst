Scrooge thrift linter
=====================

Scrooge thrift linter enforces guidelines for thrift files. These guidelines make sharing thrift files between projects easy.

Running the linter
------------------------

To run the linter, pass the '--language lint' flag into the scrooge command line with the linter jar on the classpath.
For example:
::

   ./sbt 'scrooge-linter/run /path/to/thrift/file.thrift'

List of rules
-------------

The current rules are:

1. Each thrift file must have a scala and a java namespace.

2. No relative includes ouside the current directory, i.e. no '..' in included paths.

   Absolute paths are preferred. Including files in the subdirectories is fine.

3. Struct names must be UpperCamelCase.

   Field names must be lowerCamelCase.

4. No default values for required fields.

5. Struct and field names must not be keywords in Scala, Java, Ruby, Python, PHP.
