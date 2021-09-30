package com.twitter.scrooge.backend

import com.twitter.conversions.DurationOps._
import com.twitter.scrooge.testutil.Spec
import com.twitter.util.Await
import com.twitter.util.Future

class NamespaceSpec extends Spec {
  "Scala Generator" should {
    import bar._
    import com.fake._
    "import from another namespace" in {
      val service: Restaurant.MethodPerEndpoint = new Restaurant.MethodPerEndpoint {
        def isOpen(whichDay: Weekday) = Future.value(whichDay != Weekday.Monday)
        def makeReservation(whichDay: Weekday, howMany: Int) =
          Future.value(if (whichDay == Weekday.Monday) 0 else howMany)
      }
      Await.result(service.makeReservation(Weekday.Monday, 2), 5.seconds) must equal(0)
      Await.result(service.makeReservation(Weekday.Tuesday, 2), 5.seconds) must equal(2)
    }
  }
}
