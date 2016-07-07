package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import com.github.nikalaikina.poehali.sp.Direction

case class Flight(direction: Direction, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long)

class TripRoute(val firstCity: String, val firstDate: LocalDate) {
  var flights: List[Flight] = List()

  def this(node: TripRoute, flight: Flight) {
    this(node.firstCity, node.firstDate)
    flights = node.flights :+ flight
  }

  def days = if (flights.isEmpty) 0L else DAYS.between(firstDate, flights.last.date)

  def cities(except: List[String]) = {
    flights.map(_.direction.to).distinct.count(c => !except.contains(c))
  }

  def cost = flights.map(_.price).sum

  def curCity = if (flights.isEmpty) firstCity else flights.last.direction.to

  def curDate = if (flights.isEmpty) firstDate else flights.last.date

  override def toString = s"${flights.size} $cost\t$flights"
}
