package com.twitter.scrooge.backend

import com.twitter.scrooge.mustache.Dictionary
import com.twitter.scrooge.mustache.Dictionary._
import com.twitter.scrooge.ast.{Enum, Identifier}

trait EnumTemplate {
  self: Generator =>
  def enumDict(
                namespace: Identifier,
                enum: Enum
                ): Dictionary =
    Dictionary(
      "package" -> genID(namespace),
      "EnumName" -> genID(enum.sid.toTitleCase),
      "docstring" -> codify(enum.docstring.getOrElse("")),
      "values" -> v(enum.values map {
        value =>
          Dictionary(
            "valuedocstring" -> codify(value.docstring.getOrElse("")),
            "name" -> genID(value.sid),
            "unquotedNameLowerCase" -> codify(value.sid.fullName.toLowerCase),
            "value" -> codify(value.value.toString)
          )
      }),
      "date" -> codify(generationDate)
    )
}
