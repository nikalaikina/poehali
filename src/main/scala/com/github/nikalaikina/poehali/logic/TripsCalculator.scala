package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message
import com.github.nikalaikina.poehali.message.{GetFlights, GetPlaces, GetRoutees, Routes}
import com.github.nikalaikina.poehali.model._

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Success

class TripsCalculator() extends AbstractActor {

  var trip: Trip = _
  var sender_ = Actor.noSender
  var routes = new ListBuffer[TripRoute]()
  val count = new AtomicInteger()
  val flightsProvider = context
    .actorSelection("/user/flightsProvider")
  val citiesProvider = context // TODO
    .actorSelection("/user/placesProvider")

  var cities: List[Airport] = _

  private def isFine(route: TripRoute): Boolean = {
    (route.flights.size > 1
      && trip.homeCities.contains(nameById(route.curAirport))
      && route.cost < trip.cost
      && route.citiesCount >= trip.citiesCount
      && route.days >= trip.daysFrom
      && route.days <= trip.daysTo)
  }

  private def processNode(current: TripRoute): Unit = {
    if (isFine(current)) {
      routes.synchronized { routes += current }
    } else if (current.days < trip.daysTo && current.cost < trip.cost) {
      for (city <- trip.cities; if nameById(current.curAirport) != city) {
        count.incrementAndGet()
        getFlights(current, city)
          .map(_.filter(_.price < trip.cost).distinct)
          .onComplete {
            case Success(List()) =>
              nodeProcessed()
            case Success(flights) =>
              flights.foreach(flight => processNode(new TripRoute(current, flight)))
              nodeProcessed()
            case x => throw new RuntimeException(x.toString)
          }
      }
    }
  }

  def nodeProcessed() = {
    if (count.decrementAndGet() == 0) {
      sender_ ! Routes(routes.sortBy(_.cost).toList)
      context.stop(self)
    }
  }

  private def getFlights(route: TripRoute, cityName: String): Future[List[Flight]] = {

    def getAirportFlights(from: AirportId, to: AirportId): Future[List[Flight]] = {
      flightsProvider.ask(fpMessage(from, to)).mapTo[List[Flight]]
    }

    def fpMessage(from: AirportId, to: AirportId): message.GetFlights = {
      GetFlights(Direction(from, to), route.curDate.plusDays(2), route.curDate.plusDays(trip.daysTo))
    }

    val fromIds = cities.filter(_.id == route.curAirport).map(_.city).flatMap(idsByName)
    val toIds = cities.filter(_.city == cityName).map(_.id)
    Future.sequence(for (from <- fromIds; to <- toIds) yield getAirportFlights(from, to)).map(_.flatten)
  }

  def getFirstDays: IndexedSeq[LocalDate] = {
    val from = trip.dateFrom
    val to = trip.dateTo.minusDays(trip.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }

  def getAllAirports(cityId: String) = {
    cities.filter(_.id == cityId).map(_.city).flatMap(name => cities.filter(_.city == name)).map(_.id)
  }

  def nameById(airport: AirportId) = cities.find(_.id == airport).map(_.city).getOrElse(throw new Exception(s"No city with id=${airport.id}"))
  def idsByName(city: String) = cities.filter(_.city == city).map(_.id)

  override def receive: Receive = {
    case GetRoutees(tripSettings) =>
      trip = tripSettings
      sender_ = sender()
      citiesProvider.ask(GetPlaces(100000))
        .mapTo[List[Airport]]
        .foreach(list => {
          cities = list
          val homeAirports = trip.homeCities.flatMap(idsByName)
          for (city <- homeAirports; day <- getFirstDays) {
            processNode(new TripRoute(city, day))
          }
        })
  }
}

object TripsCalculator {
  def logic()(implicit context: ActorContext) = {
    context.actorOf(Props(classOf[TripsCalculator]))
  }
}