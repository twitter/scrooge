package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.CocoaGeneratorFactory

class CocoaGoldFileTest extends GoldFileTest {

  protected def language: String = CocoaGeneratorFactory.language

}
