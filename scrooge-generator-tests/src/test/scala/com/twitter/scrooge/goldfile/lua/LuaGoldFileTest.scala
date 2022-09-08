package com.twitter.scrooge.goldfile.lua

import com.twitter.scrooge.backend.lua.LuaGeneratorFactory
import com.twitter.scrooge.goldfile.GoldFileTest

class LuaGoldFileTest extends GoldFileTest {

  /*
   * The gold files can be regenerated via the command
   * source $ ./pants run scrooge-internal/generator:bin --jvm-run-jvm-program-args="--language lua --dest scrooge/scrooge-generator-tests/src/test/resources/gold_file_output_lua/ scrooge/scrooge-generator-tests/src/test/resources/gold_file_input/gold.thrift"
   */

  protected def language: String = LuaGeneratorFactory.language

  override def testThriftFiles: Seq[String] =
    super.testThriftFiles :+ "test_thrift/lua.thrift"
}
