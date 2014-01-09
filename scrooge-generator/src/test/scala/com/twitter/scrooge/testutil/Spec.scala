package com.twitter.scrooge.testutil

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.{JMockCycle, JMockCycleFixture, MockitoSugar}
import org.scalatest.fixture.{WordSpec => FixtureWordSpec}

@RunWith(classOf[JUnitRunner])
abstract class Spec extends WordSpec with MustMatchers with MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class JMockSpec extends FixtureWordSpec with MustMatchers with JMockCycleFixture
