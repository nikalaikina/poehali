package com.github.nikalaikina

import java.io.PrintWriter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Logic(val settings: Settings) {
  val flightsProvider = new FlightsProvider(settings.cities, settings.dateFrom, settings.dateTo)

  def writeAnswer(writer: PrintWriter) = {
    var queue = mutable.Queue[Route]()
    queue ++= (for (city <- settings.homeCities; day <- getFirstDays) yield new Route(city, day))
    var routes = new ListBuffer[Route]()

    while (queue.nonEmpty) {
      val current = queue.dequeue
      val day = current.days
      if (isFine(current)) {
        routes += current
      } else if (day < settings.daysTo && current.cost < settings.cost) {
        processNode(queue, current)
      }
    }

    routes.sortBy(r => r.cost).foreach(r => writer.println(r))
  }

  private def isFine(route: Route): Boolean = {
    (route.flights.size > 3
      && settings.homeCities.contains(route.curCity)
      && route.cost < settings.cost
      && route.cities(settings.homeCities) >= settings.citiesCount
      && route.days >= settings.daysFrom
      && route.days <= settings.daysTo)
  }

  private def processNode(queue: mutable.Queue[Route], current: Route) = {
    for (city <- settings.cities) {
      if (!(current.curCity == city)) {
        val flights = getFlights(current, city)
        if (flights.nonEmpty) {
          queue += new Route(current, flights.minBy(f => f.price))
        }
      }
    }
  }

  private def getFlights(route: Route, city: String) = {
    flightsProvider.getFlights(route.curCity, city, route.curDate.plusDays(2), route.curDate.plusDays(settings.daysTo))
  }

  private def getFirstDays: IndexedSeq[LocalDate] = {
    val from = settings.dateFrom
    val to: LocalDate = settings.dateTo.minusDays(settings.daysFrom)
    val n = ChronoUnit.DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }
}
