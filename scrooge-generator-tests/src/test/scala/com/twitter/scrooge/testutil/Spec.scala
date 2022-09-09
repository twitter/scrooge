package com.twitter.scrooge.testutil

import com.twitter.util.mock.Mockito
import org.scalatest.fixture.{WordSpec => FixtureWordSpec}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.jmock.JMockCycleFixture

abstract class Spec extends AnyWordSpec with Matchers with Mockito

abstract class JMockSpec extends FixtureWordSpec with Matchers with JMockCycleFixture
