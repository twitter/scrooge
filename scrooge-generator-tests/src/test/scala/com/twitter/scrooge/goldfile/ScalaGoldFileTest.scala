package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.ScalaGeneratorFactory

class ScalaGoldFileTest extends GoldFileTest {

  /*
   * The gold files can be regenerated via the command
   * source $ ./pants run scrooge-internal/generator:bin --jvm-run-jvm-program-args="--language scala --finagle --java-passthrough --gen-adapt --dest scrooge/scrooge-generator-tests/src/test/resources/gold_file_output_scala/ scrooge/scrooge-generator-tests/src/test/resources/gold_file_input/gold.thrift"
   */

  protected def language: String = ScalaGeneratorFactory.language

}
