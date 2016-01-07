package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.ScalaGeneratorFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScalaGoldFileTest extends GoldFileTest {

  protected def language: String = ScalaGeneratorFactory.language

}
