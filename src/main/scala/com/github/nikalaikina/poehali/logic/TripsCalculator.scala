package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message
import com.github.nikalaikina.poehali.message.{GetFlights, GetRoutees, Routes}
import com.github.nikalaikina.poehali.model.{Direction, Flight, Trip, TripRoute}

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

class TripsCalculator(val trip: Trip) extends AbstractActor {

  var sender_ = Actor.noSender
  var routes = new ListBuffer[TripRoute]()
  val count = new AtomicInteger()
  val flightsProvider = context
    .actorSelection("/user/flightsProvider")
    .resolveOne(100 second)

  private def isFine(route: TripRoute): Boolean = {
    (route.flights.size > 1
      && trip.homeCities.contains(route.curCity)
      && route.cost < trip.cost
      && route.citiesCount(trip.homeCities) >= trip.citiesCount
      && route.days >= trip.daysFrom
      && route.days <= trip.daysTo)
  }

  private def processNode(current: TripRoute): Unit = {
    if (isFine(current)) {
      routes.synchronized { routes += current }
    } else if (current.days < trip.daysTo && current.cost < trip.cost) {
      for (city <- trip.cities; if current.curCity != city) {
        count.incrementAndGet()
        getFlights(current, city)
          .map(flights => (current, flights))
          .onComplete {
            case Success((route, List())) =>
              nodeProcessed()
            case Success((route, flights)) =>
              processNode(new TripRoute(current, flights.minBy(_.price)))
              nodeProcessed()
            case x => throw new RuntimeException(x.toString)
          }
      }
    }
  }

  def nodeProcessed(): Unit = {
    if (count.decrementAndGet() == 0) {
      sender_ ! Routes(routes.sortBy(_.cost).toList)
      context.stop(self)
    }
  }

  private def getFlights(route: TripRoute, city: String): Future[List[Flight]] = {
    flightsProvider.flatMap(fp => (fp ? fpMessage(route, city)).mapTo[List[Flight]])
  }

  def fpMessage(route: TripRoute, city: String): message.GetFlights = {
    GetFlights(Direction(route.curCity, city), route.curDate.plusDays(2), route.curDate.plusDays(trip.daysTo))
  }

  private def getFirstDays: IndexedSeq[LocalDate] = {
    val from = trip.dateFrom
    val to = trip.dateTo.minusDays(trip.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }

  override def receive: Receive = {
    case GetRoutees =>
      sender_ = sender()
      for (city <- trip.homeCities; day <- getFirstDays)
        processNode(new TripRoute(city, day))
  }
}

object TripsCalculator {
  def logic(trip: Trip)(implicit context: ActorContext) = {
    context.actorOf(Props(classOf[TripsCalculator], trip))
  }
}