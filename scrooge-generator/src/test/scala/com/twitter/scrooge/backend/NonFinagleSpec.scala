package com.twitter.scrooge.backend

import com.twitter.util.{Return, Throw, Try}
import org.specs.SpecificationWithJUnit

class NonFinagleSpec extends SpecificationWithJUnit {
  "None Finagle Service " should {
    "work in Scala" in {
      import vanilla.test._
      import vanilla.test1._
      import vanilla.test2._
      val chicago = Airport("ORD", "Chicago", Location(123, 321))
      val nyc = Airport("JFK", "New York", Location(789, 987))
      val service: ExtendedAirportService[Try] = new ExtendedAirportService[Try] {
        def fetchAirportsInBounds(nw: Location, se: Location) =
          Try {
            Seq(chicago, nyc)
              .filter { airport =>
                inRegion(airport.loc, nw, se)
            }
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
      service.hasWifi(chicago)() mustEqual true
      service.hasWifi(nyc)() mustEqual false
      val sfo = Airport("SFO", "San Francisco", Location(10, 10))
      service.hasWifi(sfo)() must throwA[AirportException]
      service.fetchAirportsInBounds(Location(500,0), Location(0, 500))() mustEqual Seq(chicago)
    }

    "work in Java" in {
      import vanilla_java.test._
      import vanilla_java.test1._
      import vanilla_java.test2._
      import scala.collection.JavaConversions._

      val chicago = new Airport("ORD", "Chicago", new Location(123, 321))
      val nyc = new Airport("JFK", "New York", new Location(789, 987))
      val service: ExtendedAirportService.Iface = new ExtendedAirportService.Iface {
        def fetchAirportsInBounds(nw: Location, se: Location) =
          Seq(chicago, nyc)
            .filter { airport =>
            inRegion(airport.getLoc(), nw, se)
          }
        def hasWifi(a: Airport) =
          if (a.getCode() == "ORD")
            true
          else if (a.getCode() == "JFK")
            false
          else
            throw new AirportException(100)

        private[this] def inRegion(loc: Location, nw: Location, se: Location) =
          loc.getLatitude() < nw.getLatitude() &&
            loc.getLatitude() > se.getLatitude() &&
            loc.getLongitude() > nw.getLongitude() &&
            loc.getLongitude() < se.getLongitude()
      }
      service.hasWifi(chicago) mustEqual true
      service.hasWifi(nyc) mustEqual false
      val sfo = new Airport("SFO", "San Francisco", new Location(10, 10))
      service.hasWifi(sfo) must throwA[AirportException]
      service.fetchAirportsInBounds(new Location(500,0), new Location(0, 500)).toSeq mustEqual Seq(chicago)
    }
  }
}
