package com.twitter.scrooge.testutil

import org.junit.runner.RunWith
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.{JMockCycleFixture, MockitoSugar}
import org.scalatest.fixture.{WordSpec => FixtureWordSpec}
import com.twitter.scrooge.java_generator.test.ApacheCompatibilityHelpers

@RunWith(classOf[JUnitRunner])
abstract class Spec extends WordSpec with MustMatchers with MockitoSugar

@RunWith(classOf[JUnitRunner])
abstract class JMockSpec extends FixtureWordSpec with MustMatchers with JMockCycleFixture

object Utils extends WordSpec with MustMatchers {
  def verify(actual: String, expected: String, cleanEmptySemicolons: Boolean = true) {
    val actualItems = ApacheCompatibilityHelpers.cleanWhitespace(actual, cleanEmptySemicolons)
    val expectedItems = ApacheCompatibilityHelpers.cleanWhitespace(expected, cleanEmptySemicolons)
    for (i <- 0 until actualItems.size) {
      if (!actualItems(i).equals(expectedItems(i))) {
        println("Actual: " + actualItems(i))
        println("Expected: " + expectedItems(i))
        // No idea why the one below doesn't make the test red
        assert(actualItems(i) == expectedItems(i))
      } else {
        // println(expectedItems(i))
      }
      actualItems(i) must be(expectedItems(i))
    }
  }
}
