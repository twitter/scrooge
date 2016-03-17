package com.twitter.scrooge

import org.apache.thrift.TEnum

trait ThriftEnum extends TEnum {
  def value: Int
  def name: String
  def originalName: String = name
  def getValue = value
}
