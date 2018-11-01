package com.twitter.scrooge.backend

import com.twitter.scrooge.frontend.ParseException

private[backend] class ConstructionRequiredAnnotationException(field: String)
    extends ParseException(
      s"""The construction-required annotation was found on a non-optional field: $field"""
    )
