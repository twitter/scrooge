package com.twitter.scrooge.goldfile

import com.twitter.scrooge.backend.node.NodeGeneratorFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeGoldFileTest extends GoldFileTest {

  protected def language: String = NodeGeneratorFactory.language

}
