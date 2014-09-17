# Scrooge
[![Build Status](https://secure.travis-ci.org/twitter/scrooge.png)](https://travis-ci.org/twitter/scrooge)

Scrooge is a [thrift](https://thrift.apache.org/) code generator written in
Scala, which currently generates code for Scala and Java.

It's meant to be a replacement for the apache thrift code generator, and
generates conforming, compatible binary codecs by building on top of
libthrift.

Since Scala is API-compatible with Java, you can use the apache thrift code
generator to generate Java files and use them from within Scala, but the
generated code uses Java collections and mutable "bean" classes, causing some
annoying boilerplate conversions to be hand-written. This is an attempt to
bypass the problem by generating Scala code directly. It also uses Scala
syntax so the generated code is much more compact.

There is a fairly comprehensive set of unit tests, which actually generate
code, compile it, and execute it to verify expectations.

## Quick-start

There are a couple of classes needed by the generated code. These have been
moved out of scrooge into a separate jar to keep dependencies small.
Maven users need to add the following to the pom.xml file:

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>scrooge-core_2.9.2</artifactId>
      <version>3.3.2</version>
    </dependency>

SBT users need this:

    val scroogeCore = "com.twitter" %% "scrooge-core" % "3.3.2"

## Full Documentation

<https://twitter.github.io/scrooge/>
