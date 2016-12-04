package com.github.nikalaikina.poehali.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import info.mukel.telegrambot4s.models.Location

case class AirportId(id: String) {
  if (id.head.isUpper && id.length > 3) {
    throw new Exception(s"$id is not an airport id")
  }
}

case class Direction(from: AirportId, to: AirportId)

case class CityDirection(from: String, to: String) {
  def woDirection = if (from < to) {
    CityDirection(from, to)
  } else {
    CityDirection(to, from)
  }
}

trait Ticket {
  val direction: CityDirection
  val price: Float
  val date: LocalDate
  val timeFrom: Long
  val timeTo: Long
  val url: String
}

case class BusTicket(direction: CityDirection, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long, url: String) extends Ticket

case class Flight(airports: Direction, direction: CityDirection, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long, routes: List[Route], url: String, duration: String) extends Ticket {
  override def equals(o: scala.Any): Boolean = o match {
    case that: Flight => airports.equals(that.airports) && price.equals(that.price) && date.equals(that.date)
    case _ => false
  }
}

case class Airport(id: AirportId, city: String, score: Int, location: Location)

case class Route(flight_no: Long, airline: String, lngFrom: Float, latFrom: Float, aTimeUTC: Long, dTimeUTC: Long, latTo: Float, lngTo: Float, flyFrom: String, flyTo: String, cityFrom: String, cityTo: String, aTime: Long, dTime: Long)

case class TripRoute(flights: List[Flight]) {

  val firstDate = flights.head.date

  lazy val days = DAYS.between(firstDate, flights.last.date)

  val cost = flights.map(_.price).sum

  val curCity = flights.last.direction.to

  val curDate = flights.last.date

  override val toString = s"${flights.size} $cost\t$flights"
}
