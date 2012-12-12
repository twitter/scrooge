package com.twitter.scrooge.backend

import org.specs.SpecificationWithJUnit

class NamespaceSpec extends SpecificationWithJUnit {
  "Scala Generator" should {
    import foo._
    import bar._
    import com.fake._
    "import from another namespace" in {
      val service: Restaurant.Iface = new Restaurant.Iface {
        def isOpen(whichDay: Weekday): Boolean = whichDay != Weekday.Monday
        def makeReservation(whichDay: Weekday, howMany: Int): Int =
          if (whichDay == Weekday.Monday) 0 else howMany
      }
      service.makeReservation(Weekday.Monday, 2) mustEqual 0
      service.makeReservation(Weekday.Tuesday, 2) mustEqual 2
    }
  }

  "Java Generator" should {
    import java_foo._
    import java_bar._
    import com.java_fake._
    "import from another namespace" in {
      val service: Restaurant.Iface = new Restaurant.Iface {
        def isOpen(whichDay: Weekday) = whichDay != Weekday.MONDAY
        def makeReservation(whichDay: Weekday, howMany: Int) =
          if (whichDay == Weekday.MONDAY) 0 else howMany
      }
      service.makeReservation(Weekday.MONDAY, 2) mustEqual 0
      service.makeReservation(Weekday.TUESDAY, 2) mustEqual 2
    }
  }
}
