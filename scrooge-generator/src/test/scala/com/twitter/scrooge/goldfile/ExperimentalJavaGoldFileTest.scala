package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.JavaGeneratorFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExperimentalJavaGoldFileTest extends GoldFileTest {

  protected def language: String = JavaGeneratorFactory.language

}
