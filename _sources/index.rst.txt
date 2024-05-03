Scrooge
=======

.. image:: _static/scrooge.png
   :class: floatingflask

Scrooge is a `thrift <https://thrift.apache.org/>`_ code generator written in
Scala, which currently generates code for Scala, Java, Cocoa, Android and Lua.

It's meant to be a replacement for the apache thrift code generator, and
generates conforming, binary-compatible codecs by building on top of
libthrift.

Since Scala is API-compatible with Java, you can use the apache thrift code
generator to generate Java files and use them from within Scala, but the
generated code uses Java collections and mutable "bean" classes, causing some
annoying boilerplate conversions to be hand-written. This is an attempt to
bypass the problem by generating Scala code directly. It also uses Scala
syntax so the generated code is much more compact.

There is a fairly comprehensive set of unit tests, which actually generate
code, compile it, and execute it to verify expectations.

Features
--------

- Generates native Scala thrift codecs

- Generated code is templated using a mustache variant, making it easy to
  edit.

- Finagle client/server adaptors can be optionally generated at the same time.

- Has a pluggable backend providing a dynamic way to add more generator targets.

Using Scrooge
-------------

There are a couple of classes needed by the generated code. These have been
moved out of scrooge into a separate jar to keep dependencies small.
Maven users need to add the following to the pom.xml file:

::

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>scrooge-core_2.12</artifactId>
      <version>24.2.0</version>
    </dependency>

SBT users need this:

::

    val scroogeCore = "com.twitter" %% "scrooge-core" % "24.2.0"

Building Scrooge
----------------

To build scrooge, use sbt:

::

    $ ./sbt publishLocal

This will currently not build and publish the scrooge-sbt-plugin.
You can still build the scrooge-sbt-plugin separately by executing:

::

    $ ./sbt
    > project scrooge-sbt-plugin
    > publishLocal

User's guide
------------

.. toctree::
   :maxdepth: 4

   Namespaces
   SBTPlugin
   MVNPlugin
   CommandLine
   Finagle
   CodeGenDep
   GeneratedCodeUsage
   Semantics
   Linter
   SwiftUserGuide
   ThriftValidation

Notes
-----

.. toctree::
   :maxdepth: 2

   changelog
   license
