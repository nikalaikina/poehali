package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

import akka.actor.{Actor, ActorContext, Props}
import com.github.nikalaikina.poehali.api.Trip
import com.github.nikalaikina.poehali.mesagge.{GetRoutees, Routes}
import com.github.nikalaikina.poehali.sp.{Direction, FlightsProvider}

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Logic(val settings: Trip, val flightsProvider: FlightsProvider) extends Actor {

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
    (route.flights.size > 1
      && settings.homeCities.contains(route.curCity)
      && route.cost < settings.cost
      && route.citiesCount(settings.homeCities) >= settings.citiesCount
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
    flightsProvider.getFlights(Direction(route.curCity, city), route.curDate.plusDays(2), route.curDate.plusDays(settings.daysTo))
  }

  private def getFirstDays: IndexedSeq[LocalDate] = {
    val from = settings.dateFrom
    val to = settings.dateTo.minusDays(settings.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }

  override def receive: Receive = {
    case GetRoutees =>
      sender() ! Routes(answer())
      context.stop(self)
  }
}

object Logic {
  def logic(settings: Trip, flightsProvider: FlightsProvider)(implicit context: ActorContext)
  = context.actorOf(Props(classOf[Logic], settings, flightsProvider))
}