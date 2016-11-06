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

case class CityDirection(from: String, to: String)

trait Ticket {
  val direction: CityDirection
  val price: Float
  val date: LocalDate
  val timeFrom: Long
  val timeTo: Long
  val url: String
}

case class BusTicket(direction: CityDirection, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long, url: String) extends Ticket

case class Flight(airports: Direction, direction: CityDirection, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long, routes: List[Route], url: String) extends Ticket {
  override def equals(o: scala.Any): Boolean = o match {
    case that: Flight => airports.equals(that.airports) && price.equals(that.price) && date.equals(that.date)
    case _ => false
  }
}

case class Airport(id: AirportId, city: String, score: Int, location: Location)

case class Route(flightNo: Long, airline: String)

class TripRoute(val firstCity: String, val firstDate: LocalDate) {
  var flights: List[Flight] = List()

  def this(node: TripRoute, flight: Flight) {
    this(node.firstCity, node.firstDate)
    flights = node.flights :+ flight
  }

  def days = if (flights.isEmpty) 0L else DAYS.between(firstDate, flights.last.date)

  def cost = flights.map(_.price).sum

  def curCity = if (flights.isEmpty) firstCity else flights.last.direction.to

  def curDate = if (flights.isEmpty) firstDate else flights.last.date

  override def toString = s"${flights.size} $cost\t$flights"
}
