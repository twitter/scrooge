package com.twitter.scrooge

import org.apache.thrift.TEnum

abstract class ThriftEnum(val value: Int, val name: String) extends TEnum {
  def getValue = value
}
