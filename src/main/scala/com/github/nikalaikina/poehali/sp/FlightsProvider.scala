package com.github.nikalaikina.poehali.sp

import java.lang.Class
import java.time.LocalDate

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.GetFlights
import com.github.nikalaikina.poehali.model.{CityDirection, Flight}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.{Codec, GZippingBinaryCodec, InMemoryRepr}
import scalacache.{ScalaCache, sync}
import scala.reflect._
import scala.util.{Failure, Success}


trait FlightsProvider { actor: AbstractActor with AskSupport =>

  import scalacache._
  implicit val flags = Flags.defaultFlags
  implicit val cc: GZippingBinaryCodec[List[Flight]] = scalacache.serialization.GZippingJavaAnyBinaryCodec.default[List[Flight]]

  implicit val citiesCache: ScalaCache[Array[Byte]]
  val spApi: ActorRef

  def getFlights(direction: CityDirection, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    getFlightsCached(direction).filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo))
  }

  def getFlightsCached(direction: CityDirection): List[Flight] =
    sync.cachingWithTTL(direction)(24 hours) {
      val dateFrom = LocalDate.now()
      val dateTo = dateFrom.plusYears(1)
      val f = (spApi ? GetFlights(direction, dateFrom, dateTo))
        .mapTo[List[Flight]]
      val result = Await.ready(f, Duration.Inf).value.get

      result match {
        case Success(t) => t
        case Failure(e) => List()
      }
    }

  override def receive: Receive = {
    case x =>
      log.error(s"Received a message: $x")
  }
}
