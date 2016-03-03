package com.twitter.scrooge.goldfile

import com.twitter.scrooge.java_generator.ApacheJavaGeneratorFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ApacheJavaGoldFileTest extends GoldFileTest {

  protected def language: String = ApacheJavaGeneratorFactory.language

}
