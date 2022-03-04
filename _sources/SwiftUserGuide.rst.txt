Swift User Guide
================

This section explains how to work with the Swift file generator.


Scrooge supports generation of the Swift model classes. The thrift files need to
have this namespace definition: ``#@namespace swift ThriftEncoding``. Scrooge
creates the models as a Swift package with that namespace as the name.

The generated swift uses `TwitterApacheThrift<https://github.com/twitter/ios-twitter-apache-thrift>`.

Objective-C Compatibility
~~~~~~~~~~~~~~~~~~~~~~~~~
The default is to create Swift only types. Scrooge has the ability to generate
the models with Objective-C compatibility. This command line argument is
``--language-flag 'swift-objc prefix'``, with the prefix being the Obj-C class
prefix. **Note: The apostrophes are required.** Enums will add @objc and structs
will become NSObject classes. Unions will be Enums with raw type wrapped in an
NSObject class with initializers for each case. The swift encoder and decoder
classes will remain Swift only. This allows this library to be a drop-in
replacement for current ApacheThrift library.

Swift Classes
~~~~~~~~~~~~~
Scrooge can also generate non-NSObject classes instead of structs. To enable
this pass ``--language-flag swift-classes`` to scrooge generation command.

.. note::

  When passing both the objective-c compatibility and swift class arguments. The
  swift class argument does nothing.

Access Control
~~~~~~~~~~~~~~
Scrooge can also generate for internal interfaces instead of public interfaces.
To enable this pass ``--language-flag internal-types`` to scrooge generation
command.

Type naming
~~~~~~~~~~~
Swift doesn’t support namespaces the same way as JVM languages. Scrooge uses an
annotation on the thrift models to generate different type names. These new
annotations will be available for Structs, Unions, and Enums. The following
thrift will create a swift Struct with the name of UserV1.

.. code-block:: thrift

    struct User {
      1: required i64 id
    }(alternative.type = 'UserV1')

.. code-block:: swift

    // Generated from thrift
    public struct UserV1: ThriftCodable {
      let id: Int64
      ...
    }

Generation Command
~~~~~~~~~~~~~~~~~~
Here is some example command to generate the swift models from thrift.

.. code-block:: bash

  # Generates swift structs
  $ ./sbt 'scrooge-generator/runMain com.twitter.scrooge.Main -l swift -d generated -s your_file.thrift'

  # Generates swift with obj-c compatibility
  $ ./sbt "scrooge-generator/runMain com.twitter.scrooge.Main -d generated -l swift --language-flag 'swift-objc prefix' -s your_file.thrift"

  # Generates swift with classes instead of structs
  $ ./sbt 'scrooge-generator/runMain com.twitter.scrooge.Main -d generated -l swift --language-flag swift-classes -s your_file.thrift'
