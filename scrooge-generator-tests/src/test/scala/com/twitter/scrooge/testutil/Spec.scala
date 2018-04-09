package com.twitter.scrooge.testutil

import org.junit.runner.RunWith
import org.scalatest.fixture.{WordSpec => FixtureWordSpec}
import org.scalatest.jmock.JMockCycleFixture
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}

import com.twitter.scrooge.java_generator.test.ApacheCompatibilityHelpers

@RunWith(classOf[JUnitRunner])
abstract class Spec extends WordSpec with MustMatchers with MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class JMockSpec extends FixtureWordSpec with MustMatchers with JMockCycleFixture
