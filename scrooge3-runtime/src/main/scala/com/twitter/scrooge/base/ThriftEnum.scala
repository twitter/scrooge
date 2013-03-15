package com.twitter.scrooge.base

import org.apache.thrift.TEnum

trait ThriftEnum extends TEnum {
  def value: Int
  def name: String
  def getValue = value
}
