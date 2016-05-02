package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import com.github.nikalaikina.poehali.api.Settings
import com.github.nikalaikina.poehali.sp.FlightsProvider

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Logic(val settings: Settings) {
  val flightsProvider = new FlightsProvider(settings.cities, settings.dateFrom, settings.dateTo)

  def answer(): List[TripRoute] = {
    var queue = mutable.Queue[TripRoute]()
    queue ++= (for (city <- settings.homeCities; day <- getFirstDays) yield new TripRoute(city, day))
    var routes = new ListBuffer[TripRoute]()

    while (queue.nonEmpty) {
      val current = queue.dequeue
      if (isFine(current)) {
        routes += current
      } else if (current.days < settings.daysTo && current.cost < settings.cost) {
        processNode(queue, current)
      }
    }

    routes.sortBy(_.cost).toList
  }

  private def isFine(route: TripRoute): Boolean = {
    (route.flights.size > 2
      && settings.homeCities.contains(route.curCity)
      && route.cost < settings.cost
      && route.cities(settings.homeCities) >= settings.citiesCount
      && route.days >= settings.daysFrom
      && route.days <= settings.daysTo)
  }

  private def processNode(queue: mutable.Queue[TripRoute], current: TripRoute) = {
    for (city <- settings.cities; if current.curCity != city) {
      val flights = getFlights(current, city)
      if (flights.nonEmpty) {
        queue += new TripRoute(current, flights.minBy(_.price))
      }
    }
  }

  private def getFlights(route: TripRoute, city: String) = {
    flightsProvider.getFlights(route.curCity, city, route.curDate.plusDays(1), route.curDate.plusDays(settings.daysTo))
  }

  private def getFirstDays: IndexedSeq[LocalDate] = {
    val from = settings.dateFrom
    val to = settings.dateTo.minusDays(settings.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }
}
