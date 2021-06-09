package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.ScalaGeneratorFactory

class ScalaGoldFileTest extends GoldFileTest {

  protected def language: String = ScalaGeneratorFactory.language

}
