package com.github.nikalaikina.poehali.logic

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.{GetRoutees, Routes}
import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.sp.FlightsProvider

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scalacache.ScalaCache

case class TripsCalculator(spApi: ActorRef, cities: Cities)(implicit val citiesCache: ScalaCache[Array[Byte]])
  extends AbstractActor with FlightsProvider with Calculations {

  var trip: Trip = _
  var sender_ = Actor.noSender
  var routes = new ListBuffer[TripRoute]()

  var citiesCount = 1
  var cost = 2000f
  var precision = 1

  def addRoute(current: TripRoute): Unit = {
    log.debug(s"Added route $current")
    routes.synchronized {
      routes += current
    }
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

  override def receive: Receive = {
    case GetRoutees(tripSettings) =>
      trip = tripSettings
      if (trip.cities.size < 4) {
        precision = Math.min(5 - trip.cities.size, 1)
      }
      sender_ = sender()
      for (city <- trip.homeCities; day <- getFirstDays) {
        processNode(new TripRoute(city, day))
      }
      val result = routes
        .filter(r => r.flights.size == citiesCount)
        .sortBy(_.cost)
        .take(30)
        .toList
      sender_ ! Routes(result)
      context.stop(self)
  }
}

object TripsCalculator {
  def logic(spApi: ActorRef, cities: Cities)(implicit citiesCache: ScalaCache[Array[Byte]], context: ActorContext) = {
    context.actorOf(Props(new TripsCalculator(spApi, cities)))
  }
}