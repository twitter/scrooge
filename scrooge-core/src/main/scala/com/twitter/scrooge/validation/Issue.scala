package com.twitter.scrooge.validation

import com.twitter.scrooge.ThriftStructFieldInfo

/**
 * An issue indicates that a thrift object does not conform to the requirements defined in the
 * thrift definition. These are returned by the validateNewInstance method in the thrift struct's
 * companion object.
 */
sealed trait Issue

/**
 * When the Issue is returned by validateNewInstance a construction required field is missing from
 * the object.
 * @param field ThriftStructFieldInfo for the missing field
 */
case class MissingConstructionRequiredField(field: ThriftStructFieldInfo) extends Issue

/**
 * When the Issue is returned by validateNewInstance a required field is missing from the object.
 * @param field ThriftStructFieldInfo for the missing field
 */
case class MissingRequiredField(field: ThriftStructFieldInfo) extends Issue
