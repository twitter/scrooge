package com.twitter.scrooge.benchmark

import com.twitter.scrooge.BinaryThriftStructSerializer
import scala.util.Random
import thrift.benchmark._

object AirlineGenerator {
  import AirportGenerator._
  val AvgStringSize = 10
  val NumAirports = 10
  val NumRoutes = 20
  val NumFlights = 20

  def buildFlight(rng: Random): Flight = {
    val id = rng.nextInt
    val name = rng.nextString(AvgStringSize)
    val from = buildAirport(rng)
    val to = buildAirport(rng)
    val departureTime = rng.nextInt
    Flight(id, name, from, to, Some(departureTime))
  }

  def buildFlights(rng: Random, num: Int): Set[Flight] =
    (0 until num).map{ _ => buildFlight(rng) }.toSet

  def buildRoutes(rng: Random, num: Int): Map[Airport, Airport] =
    (0 until num).map{ _ => (buildAirport(rng), buildAirport(rng)) }.toMap


  def buildAirline(rng: Random): Airline = {
    val name = rng.nextString(AvgStringSize)
    val headQuarter = buildAirport(rng)
    val owner = rng.nextString(AvgStringSize)
    val airports = buildAirports(rng, NumAirports).toSet
    val routes = buildRoutes(rng, NumRoutes)
    val flights = buildFlights(rng, NumFlights)
    Airline(
      name,
      Some(headQuarter),
      Some(owner),
      Some(airports),
      Some(routes),
      Some(flights))
  }

  private[this] def buildAirlines(rng: Random, num: Int): Array[Airline] =
    (0 until num).map{ _ => buildAirline(rng) }.toArray

  def buildAirlinesAndBytes(
    seed: Long,
    num: Int
  ): (Array[Airline], Array[Array[Byte]]) = {
    val rng = new Random(seed)
    val airlines = buildAirlines(rng, num)

    val ser = BinaryThriftStructSerializer(Airline)
    val bytes = airlines.map(ser.toBytes).toArray
    (airlines, bytes)
  }
}
