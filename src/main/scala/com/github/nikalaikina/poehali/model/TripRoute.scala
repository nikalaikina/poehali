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

case class Flight(direction: Direction, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long, routes: List[Route])

case class Airport(id: AirportId, city: String, score: Int, location: Location)

case class Route(flightNo: Long, airline: String)

class TripRoute(val firstAirport: AirportId, val firstDate: LocalDate) {
  var flights: List[Flight] = List()

  def this(node: TripRoute, flight: Flight) {
    this(node.firstAirport, node.firstDate)
    flights = node.flights :+ flight
  }

  def days = if (flights.isEmpty) 0L else DAYS.between(firstDate, flights.last.date)

  def citiesCount = flights.map(_.direction.to).distinct.size

  def airports = flights.map(_.direction.to).distinct.toSet

  def cost = flights.map(_.price).sum

  def curAirport = if (flights.isEmpty) firstAirport else flights.last.direction.to

  def curDate = if (flights.isEmpty) firstDate else flights.last.date

  override def toString = s"${flights.size} $cost\t$flights"
}
