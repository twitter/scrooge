package com.twitter.scrooge

import org.apache.thrift.TEnum

trait ThriftEnum extends TEnum {
  def value: Int
  def name: String

  /**
   * The original name for the enum value as defined in the input Thrift IDL file.
   */
  def originalName: String

  def getValue = value

  def annotations: Map[String, String]
}

/**
 * Base class for unknown enum items.
 * The implementations are used for backward compatibility during enum update at producer.
 */
trait EnumItemUnknown extends ThriftEnum

trait ThriftEnumObject[T <: ThriftEnum] {

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * Throws NoSuchElementException exception if the value is not found
   */
  def apply(value: Int): T

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * If the value is not found it returns a special enum unknown value
   * of type T that extends EnumItemUnknown type. In particular this allows
   * ignoring new values added to an enum in the IDL on the producer
   * side when the consumer was not updated.
   */
  def getOrUnknown(value: Int): T

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * Returns None if the value is not found
   */
  def get(value: Int): Option[T]

  /**
   * Find the enum by its name, as defined in the Thrift IDL.
   * Returns None if the value is not found
   */
  def valueOf(name: String): Option[T]

  /**
   * Returns a list of all possible enums.
   */
  def list: List[T]
}
