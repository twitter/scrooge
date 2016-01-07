package com.twitter.scrooge.goldfile

import com.twitter.scrooge.android_generator.AndroidGeneratorFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AndroidGoldFileTest extends GoldFileTest {

  protected def language: String = AndroidGeneratorFactory.language

}
