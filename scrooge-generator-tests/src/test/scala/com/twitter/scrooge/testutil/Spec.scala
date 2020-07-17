package com.twitter.scrooge.testutil

import org.junit.runner.RunWith
import org.scalatest.fixture.{WordSpec => FixtureWordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.jmock.JMockCycleFixture
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class Spec extends WordSpec with MustMatchers with MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class JMockSpec extends FixtureWordSpec with MustMatchers with JMockCycleFixture
