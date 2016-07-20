package com.github.nikalaikina.poehali.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import info.mukel.telegrambot4s.models.Location

case class Direction(from: String, to: String)

case class Flight(direction: Direction, price: Float, date: LocalDate, timeFrom: Long, timeTo: Long)

case class City(id: String, name: String, score: Int, location: Location)

class TripRoute(val firstCity: String, val firstDate: LocalDate) {
  var flights: List[Flight] = List()

  def this(node: TripRoute, flight: Flight) {
    this(node.firstCity, node.firstDate)
    flights = node.flights :+ flight
  }

  def days = if (flights.isEmpty) 0L else DAYS.between(firstDate, flights.last.date)

  def citiesCount(except: Set[String]) = {
    flights.map(_.direction.to).distinct.count(c => !except.contains(c))
  }

  def cities(except: Set[String]): Set[String] = flights.map(_.direction.to).distinct.toSet

  def cost = flights.map(_.price).sum

  def curCity = if (flights.isEmpty) firstCity else flights.last.direction.to

  def curDate = if (flights.isEmpty) firstDate else flights.last.date

  override def toString = s"${flights.size} $cost\t$flights"
}
