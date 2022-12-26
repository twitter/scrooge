# Scrooge

[![Build Status](https://github.com/twitter/scrooge/workflows/continuous%20integration/badge.svg?branch=develop)](https://github.com/twitter/scrooge/actions?query=workflow%3A%22continuous+integration%22+branch%3Adevelop)
[![Project status](https://img.shields.io/badge/status-active-brightgreen.svg)](#status)
[![Gitter](https://badges.gitter.im/twitter/finagle.svg)](https://gitter.im/twitter/finagle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.twitter/scrooge-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.twitter/scrooge-core_2.12)

Scrooge is a [thrift](https://thrift.apache.org/) code generator written in
Scala, which currently generates code for Scala, Java, Cocoa, Android and Lua.

It's meant to be a replacement for the apache thrift code generator, and
generates conforming, compatible binary codecs by building on top of
libthrift.  It integrates with the [finagle][0] project, exporting stats
and finagle APIs, and makes it easy to build high throughput, low latency,
robust thrift servers and clients.

Part of the motivation behind scrooge's scala implementation is that since Scala
is API-compatible with Java, you can use the apache thrift code generator to
generate Java files and use them from within Scala, but the generated code uses
Java collections and mutable "bean" classes, causing some annoying boilerplate
conversions to be hand-written. Scrooge bypasses the problem by generating Scala
code directly. It also uses Scala syntax so the generated code is much more
compact.

There is a comprehensive set of unit tests, which generate code, compile it, and
execute it to verify expectations, as well as gold files to make it easy to
review the effects of changes to the generator.

## Status

This project is used in production at Twitter (and many other organizations),
and is actively developed and maintained.

## Building the develop branch locally

We are not currently publishing snapshots for Scrooge's dependencies, which
means that it may be necessary to publish the `develop` branches of these
libraries locally in order to work on Scrooge's `develop` branch. To do so
you can use our build tool, [dodo](https://github.com/twitter/dodo).

``` bash
curl -s https://raw.githubusercontent.com/twitter/dodo/develop/bin/build | bash -s -- --no-test scrooge
```

If you have any questions or run into any problems, please create
an issue here, tweet at us at [@finagle](https://twitter.com/finagle), or email
the Finaglers mailing list.

## Full Documentation

<https://twitter.github.io/scrooge/>

## License

Copyright 2013 Twitter, Inc.

Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0

[0]: https://github.com/twitter/finagle
