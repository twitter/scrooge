package com.twitter.scrooge.backend

import com.twitter.scrooge.mustache.Dictionary.{codify, v}
import com.twitter.scrooge.mustache.Dictionary
import com.twitter.scrooge.ast.{Identifier, ConstDefinition}

trait ConstsTemplate {
  self: Generator =>
  def constDict(
    basename: String,
    namespace: Identifier,
    consts: Seq[ConstDefinition]
  ): Dictionary = Dictionary(
    "package" -> genID(namespace),
    "basename" -> codify(basename),
    "constants" -> v(consts map {
      c =>
        Dictionary(
          "name" -> genID(c.sid),
          "fieldType" -> genType(c.fieldType),
          "value" -> genConstant(c.value, false, Some(c.fieldType)),
          "docstring" -> codify(c.docstring.getOrElse(""))
        )
    })
  )
}
