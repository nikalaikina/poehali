package com.github.nikalaikina.poehali.sp

import java.time.LocalDate

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.logic.Flight
import com.github.nikalaikina.poehali.message.GetFlights

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.InMemoryRepr
import scalacache.{ScalaCache, sync}

case class Direction(from: String, to: String)

class FlightsProvider(spApi: ActorRef)(implicit cache: ScalaCache[InMemoryRepr]) extends AbstractActor {

  def getFlights(direction: Direction, dateFrom: LocalDate, dateTo: LocalDate): Future[List[Flight]] = {
    getFlightsCached(direction)
        .map(list => list.filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo)))
  }

  def getFlightsCached(direction: Direction): Future[List[Flight]] =
    sync.cachingWithTTL(direction)(24 hours) {
      val dateFrom = LocalDate.now()
      val dateTo = dateFrom.plusYears(1)
      (spApi ? GetFlights(direction, dateFrom, dateTo))
        .mapTo[List[Flight]]
    }

  override def receive: Receive = {
    case GetFlights(direction, dateFrom, dateTo) =>
      getFlights(direction, dateFrom, dateTo) pipeTo sender()
  }
}

object FlightsProvider {
  def props(spApi: ActorRef)(implicit cache: ScalaCache[InMemoryRepr]) = Props(new FlightsProvider(spApi))
}


