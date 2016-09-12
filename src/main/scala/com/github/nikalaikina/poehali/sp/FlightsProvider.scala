package com.github.nikalaikina.poehali.sp

import java.time.LocalDate

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.GetFlights
import com.github.nikalaikina.poehali.model.{Direction, Flight}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.InMemoryRepr
import scalacache.{ScalaCache, sync}


trait FlightsProvider { actor: AbstractActor with AskSupport =>

  implicit val citiesCache: ScalaCache[InMemoryRepr]
  val spApi: ActorRef

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

  override def receive: Receive = ???
}

