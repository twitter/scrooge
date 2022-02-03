package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast.ConstDefinition
import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.java_generator.TypeController

class PackageFileController(
  defs: Seq[ConstDefinition],
  generator: SwiftGenerator,
  ns: Option[Identifier])
    extends TypeController("Package", generator, ns) {}
