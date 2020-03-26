package com.twitter.scrooge.backend

import com.twitter.util.Try
import com.twitter.scrooge.testutil.Spec

class NonFinagleSpec extends Spec {
  "None Finagle Service " should {
    "work in Scala" in {
      import vanilla.test._
      import vanilla.test1._
      val chicago = Airport("ORD", "Chicago", Location(123, 321))
      val nyc = Airport("JFK", "New York", Location(789, 987))
      val service: ExtendedAirportService[Try] = new ExtendedAirportService[Try] {
        def fetchAirportsInBounds(nw: Location, se: Location) =
          Try {
            Seq(chicago, nyc)
              .filter { airport => inRegion(airport.loc, nw, se) }
          }
        def hasWifi(a: Airport) =
          Try {
            if (a.code == "ORD")
              true
            else if (a.code == "JFK")
              false
            else
              throw AirportException(100)
          }

        private[this] def inRegion(loc: Location, nw: Location, se: Location) =
          loc.latitude < nw.latitude &&
            loc.latitude > se.latitude &&
            loc.longitude > nw.longitude &&
            loc.longitude < se.longitude
      }
      service.hasWifi(chicago).get() must be(true)
      service.hasWifi(nyc).get() must be(false)
      val sfo = Airport("SFO", "San Francisco", Location(10, 10))
      intercept[AirportException] {
        service.hasWifi(sfo).get()
      }
      service.fetchAirportsInBounds(Location(500, 0), Location(0, 500))() must be(Seq(chicago))
    }
  }
}
