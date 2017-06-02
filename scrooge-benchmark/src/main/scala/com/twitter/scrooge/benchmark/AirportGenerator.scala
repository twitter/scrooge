package com.twitter.scrooge.benchmark

import com.twitter.scrooge.BinaryThriftStructSerializer
import scala.util.Random
import thrift.benchmark._

object AirportGenerator {

  def buildLocation(rng: Random): Location = {
     // Next double is only between 0 and 1, so times long will give us something
     // across a wide possible range

    val lat = rng.nextDouble * rng.nextLong
    val lon = rng.nextDouble * rng.nextLong
    val alt = if (rng.nextBoolean) Some(rng.nextDouble * rng.nextLong) else None
    Location(lat, lon, alt)
  }


  def buildAirport(rng: Random): Airport = {
    val code = rng.nextString(4)
    val name = rng.nextString(30)
    val state = rng.nextOptString(10)
    val country = rng.nextOptString(15)
    val closestCity = rng.nextOptString(25)
    val location = rng.maybeRng(buildLocation)
    Airport(code, name, country, state, closestCity, location)
  }

  def buildAirports(rng: Random, num: Int): Array[Airport] =
    (0 until num).map{ _ => buildAirport(rng) }.toArray

  def buildAirportsAndBytes(seed: Long, num: Int): (Array[Airport], Array[Array[Byte]]) = {
    val rng = new Random(seed)
    val airports = buildAirports(rng, num)

    val ser = BinaryThriftStructSerializer(Airport)
    val bytes = airports.map(ser.toBytes).toArray
    (airports, bytes)
  }
}
