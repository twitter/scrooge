# Scrooge

[![Build status](https://travis-ci.org/twitter/scrooge.svg?branch=develop)](https://travis-ci.org/twitter/scrooge)
[![Coverage status](https://img.shields.io/coveralls/twitter/scrooge/develop.svg)](https://coveralls.io/r/twitter/scrooge?branch=develop)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Maven Central](https://img.shields.io/maven-central/v/com.twitter/scrooge_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.twitter/scrooge_2.11)

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

## Status

This project is used in production at Twitter (and many other organizations),
and is being actively developed and maintained.

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

## Building the develop branch locally

You will need the develop branches of [util](https://github.com/twitter/util),
[ostrich](https://github.com/twitter/ostrich),
and [finagle](https://github.com/twitter/finagle).
Finagle depends on `scrooge-core`, so the order in which you build dependencies
should be:

* in util: `./sbt publish-local`
* in ostrich: `./sbt publish-local`
* in scrooge: `./sbt 'project scrooge-core' publish-local`
* in finagle: `/.sbt publish-local`

Then you can build the entire scrooge package.

## Full Documentation

<https://twitter.github.io/scrooge/>
