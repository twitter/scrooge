package com.twitter.scrooge.testutil

import org.junit.runner.RunWith
import org.scalatest.fixture.{WordSpec => FixtureWordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.jmock.JMockCycleFixture
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class Spec extends AnyWordSpec with Matchers with MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class JMockSpec extends FixtureWordSpec with Matchers with JMockCycleFixture
