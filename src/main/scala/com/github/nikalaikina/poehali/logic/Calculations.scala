package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit._

import com.github.nikalaikina.poehali.model.{CityDirection, Flight, Trip, TripRoute}
import com.github.nikalaikina.poehali.sp.FlightsProvider

import scala.collection.immutable.IndexedSeq


trait Calculations { this: FlightsProvider =>

  def trip: Trip

  def precision: Int

  def cost: Float

  def citiesCount: Int

  private def isFine(route: TripRoute): Boolean = {
    (route.flights.size > 1
      && route.cost <= cost * 2
      && trip.homeCities.contains(route.curCity)
      && route.days >= trip.daysFrom
      && route.days <= trip.daysTo)
  }

  def processNode(current: TripRoute): Unit = {
    if (isFine(current)) {
      addRoute(current)
    } else if (current.days < trip.daysTo && current.cost <= cost * 2 * current.flights.size / citiesCount) {
      val visited = current.flights.map(_.direction.to).distinct
      for (city <- trip.allCities; if current.curCity != city && !visited.contains(city)) {
        getFlights(current, city).foreach(flight => processNode(new TripRoute(current, flight)))
      }
    }
  }

  private def getFlights(route: TripRoute, cityName: String): List[Flight] = {
    getFlights(CityDirection(route.curCity, cityName), route.curDate.plusDays(2), route.curDate.plusDays(8))
      .groupBy(_.date).toList.map(t => t._2.minBy(_.price)).sortBy(_.price).take(precision)
  }

  def getFirstDays: IndexedSeq[LocalDate] = {
    val from = trip.dateFrom
    val to = trip.dateTo.minusDays(trip.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }

  def addRoute(route: TripRoute)
}
