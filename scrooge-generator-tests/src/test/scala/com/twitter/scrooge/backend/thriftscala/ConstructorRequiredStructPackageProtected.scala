package com.twitter.scrooge.backend.thriftscala
// This is in the same package as the generated thrift. It is hack to expose a method publicly that
// should only be accessible within this package.

import com.twitter.scrooge.TFieldBlob

object ConstructorRequiredStructPackageProtected {

  /**
   * Method that exposes the package private constructor for ConstructorRequiredStruct. We can use
   * this to create ConstructorRequiredStruct.Immutable that are missing constructionRequiredField.
   */
  def apply(
    optionalField: _root_.scala.Option[Long],
    requiredField: String,
    constructionRequiredField: _root_.scala.Option[Long],
    defaultRequirednessField: Long,
    _passthroughFields: Map[Short, TFieldBlob]
  ): ConstructorRequiredStruct = {
    new ConstructorRequiredStruct.Immutable(
      optionalField,
      requiredField,
      constructionRequiredField,
      defaultRequirednessField,
      None,
      _passthroughFields
    )
  }
}
