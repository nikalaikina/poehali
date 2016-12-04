package com.github.nikalaikina.poehali.logic

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.{GetRoutees, Routes}
import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.external.sp.TicketsProvider

import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scalacache.ScalaCache

case class TripsCalculator(spApi: ActorRef, trip: Trip)(implicit val cache: ScalaCache[Array[Byte]])
  extends AbstractActor with TicketsProvider with Calculations {

  var routes = new ListBuffer[TripRoute]()

  def addRoute(current: TripRoute): Unit = {
    log.debug(s"Added route $current")
    routes.synchronized {
      routes += current
    }
    updateState(current)
  }

  override def receive: Receive = {
    case GetRoutees(tripSettings) =>
      calc()
      val result = routes
        .filter(r => r.flights.size == citiesCount)
        .sortBy(_.cost)
        .take(30)
        .toList
      sender() ! Routes(result)
      context.stop(self)
  }
}

object TripsCalculator {
  def logic(spApi: ActorRef, trip: Trip)(implicit citiesCache: ScalaCache[Array[Byte]], context: ActorContext) = {
    context.actorOf(Props(new TripsCalculator(spApi, trip)))
  }
}