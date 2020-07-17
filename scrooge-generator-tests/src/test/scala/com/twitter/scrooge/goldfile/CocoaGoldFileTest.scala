package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.CocoaGeneratorFactory
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CocoaGoldFileTest extends GoldFileTest {

  protected def language: String = CocoaGeneratorFactory.language

}
