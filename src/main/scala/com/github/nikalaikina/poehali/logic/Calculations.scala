package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit._

import com.github.nikalaikina.poehali.model.{CityDirection, Flight, Trip, TripRoute}
import com.github.nikalaikina.poehali.sp.FlightsProvider

import scala.collection.immutable.IndexedSeq


trait Calculations { this: FlightsProvider =>

  val trip: Trip

  val precision = Math.max(6 - trip.cities.size, 1)

  var cost: Float = 1000f

  var citiesCount: Int = 1

  override val passengers: Int = trip.passengers


  def calc(): Unit = {
    for (city <- trip.homeCities; day <- getFirstDays) {
      processNode(new TripRoute(city, day))
    }
  }

  def updateState(current: TripRoute): Unit = {
    if (current.flights.size > citiesCount) {
      citiesCount = current.flights.size
      if (current.cost > cost) {
        cost = current.cost
      }
    }
    if (current.flights.size == citiesCount && current.cost < cost) {
      cost = current.cost
    }
  }

  private def isFine(route: TripRoute): Boolean = {
    (route.flights.size > 1
      && trip.homeCities.contains(route.curCity)
      && route.days >= trip.daysFrom
      && route.days <= trip.daysTo)
  }

  def processNode(current: TripRoute): Unit = {
    if (isFine(current)) {
      addRoute(current)
    } else if (current.days < trip.daysTo) {
      val nonVisited = trip.allCities -- current.flights.map(_.direction.to) - current.curCity
      for (city <- nonVisited) {
        getFlights(current, city).foreach(flight => processNode(new TripRoute(current, flight)))
      }
    }
  }

  private def getFlights(route: TripRoute, cityName: String): List[Flight] = {
    val flights = getFlights(CityDirection(route.curCity, cityName), route.curDate.plusDays(2), route.curDate.plusDays(8))
    val take = flights.groupBy(_.date).toList.map(t => t._2.minBy(_.price)).sortBy(_.price)
    take.take(precision)
  }

  def getFirstDays: IndexedSeq[LocalDate] = {
    val from = trip.dateFrom
    val to = trip.dateTo.minusDays(trip.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }

  def addRoute(route: TripRoute)
}
