package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.{GetRoutees, Routes}
import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.sp.FlightsProvider
import com.github.nikalaikina.poehali.to.JsonRoute
import org.java_websocket.WebSocket
import play.api.libs.json.Json

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Success
import scalacache.ScalaCache
import scalacache.serialization.InMemoryRepr

case class WsCalculator(spApi: ActorRef, socket: WebSocket, cities: Cities, trip: Trip)(implicit val citiesCache: ScalaCache[Array[Byte]])
  extends AbstractActor with FlightsProvider {

  import com.github.nikalaikina.poehali.util.JsonImplicits._

  val precision = Math.min(5 - trip.cities.size, 1)
  var citiesCount = 1
  var cost = 2000f

  val homeAirports = trip.homeCities.flatMap(city => cities.byName(city)).map(_.id)
  for (city <- homeAirports; day <- getFirstDays) {
    processNode(new TripRoute(city, day))
  }

  socket.close()

  private def isFine(route: TripRoute): Boolean = {
    (route.flights.size > 1
      && route.cost <= cost * 2
      && trip.homeCities.contains(cities.airports(route.curAirport).city)
      && route.days >= trip.daysFrom
      && route.days <= trip.daysTo)
  }

  private def processNode(current: TripRoute): Unit = {
    if (isFine(current)) {
      addRoute(current)
    } else if (current.days < trip.daysTo && current.cost <= cost * 2 * current.flights.size / citiesCount) {
      val visited = current.flights.map(_.direction.to)
        .distinct
        .map(airport => cities.airports(airport).city)
      val curCity = cities.airports(current.curAirport).city
      for (city <- trip.cities; if curCity != city && !visited.contains(city)) {
        getFlights(current, city).foreach(flight => processNode(new TripRoute(current, flight)))
      }
    }
  }

  private def getFlights(route: TripRoute, cityName: String): List[Flight] = {
    getFlights(CityDirection(cities.airports(route.curAirport).city, cityName), route.curDate.plusDays(2), route.curDate.plusDays(8))
      .groupBy(_.date).toList.map(t => t._2.minBy(_.price)).sortBy(_.price).take(precision)
  }

  def getFirstDays: IndexedSeq[LocalDate] = {
    val from = trip.dateFrom
    val to = trip.dateTo.minusDays(trip.daysFrom)
    val n = DAYS.between(from, to).toInt
    for (i <- 1 to n) yield from.plusDays(i)
  }

  def addRoute(current: TripRoute): Unit = {
    log.debug(s"Added route $current")
    socket.send(Json.toJson(JsonRoute(current.flights)).toString())
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
}
