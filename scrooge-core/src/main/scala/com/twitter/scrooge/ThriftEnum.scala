package com.twitter.scrooge

import org.apache.thrift.TEnum

trait ThriftEnum extends TEnum {
  def value: Int
  def name: String

// /**
//  * The original name for the enum value as defined in the input Thrift IDL file.
//  *
//  * The default implementation uses [[name]], but generated code
//  * should use the correct value.
//  */
// def originalName: String = name

  def getValue = value
}
