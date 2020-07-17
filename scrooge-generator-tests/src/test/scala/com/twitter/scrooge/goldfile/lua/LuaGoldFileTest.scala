package com.twitter.scrooge.goldfile.lua

import com.twitter.scrooge.backend.lua.LuaGeneratorFactory
import com.twitter.scrooge.goldfile.GoldFileTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LuaGoldFileTest extends GoldFileTest {

  protected def language: String = LuaGeneratorFactory.language

  override def testThriftFiles = super.testThriftFiles :+ "test_thrift/lua.thrift"
}
