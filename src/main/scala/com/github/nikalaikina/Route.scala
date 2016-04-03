package com.github.nikalaikina

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Route(val firstCity: String, val date: LocalDate) {
  var flights: List[Flight] = List()

  def this(node: Route, flight: Flight) {
    this(node.firstCity, node.date)
    flights = node.flights :+ flight
  }

  def days = if (flights.isEmpty) 0L else ChronoUnit.DAYS.between(date, flights.last.date)

  def cities(except: List[String]) = {
    flights.map(f => f.flyTo).distinct.count(c => !except.contains(c))
  }

  def cost = flights.map(f => f.price).sum

  def curCity = if (flights.isEmpty) firstCity else flights.last.flyTo

  override def toString = s"${flights.size} $cost\t$flights"
}
