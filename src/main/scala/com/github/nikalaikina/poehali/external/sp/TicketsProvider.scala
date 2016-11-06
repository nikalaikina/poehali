package com.github.nikalaikina.poehali.external.sp

import java.lang.Class
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.GetTickets
import com.github.nikalaikina.poehali.model.{CityDirection, Flight}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.{Codec, GZippingBinaryCodec, InMemoryRepr}
import scalacache.{ScalaCache, sync}
import scala.reflect._
import scala.util.{Failure, Success}


trait TicketsProvider { actor: AbstractActor with AskSupport =>

  import scalacache._
  implicit val flags = Flags.defaultFlags
  implicit val cc: GZippingBinaryCodec[List[Flight]] = scalacache.serialization.GZippingJavaAnyBinaryCodec.default[List[Flight]]

  implicit val citiesCache: ScalaCache[Array[Byte]]
  val spApi: ActorRef

  var time = 0L
  var n = 0

  val passengers = 1

  def getFlights(direction: CityDirection, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    val cached = getFlightsCached(direction)
    val filter = cached.filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo))
    filter
  }

  def getFlightsCached(direction: CityDirection): List[Flight] =
    sync.cachingWithTTL(direction)(24 hours) {
      val dateFrom = LocalDate.now()
      val dateTo = dateFrom.plusYears(1)
      val flights = retrieve(direction, dateFrom, dateTo, direct = false)
      if (flights.isEmpty) {
        log.error("EMPTY " + "*" * 80)
      }
      val minDirectCost = flights.filter(_.routes.size == 1).map(_.price).sortWith(_ < _).headOption.getOrElse(-1)
      val minNonDirectCost = flights.filter(_.routes.size > 1).map(_.price).sortWith(_ < _).headOption.getOrElse(-1)
      log.info(s"${direction.from}\t\t${direction.to}\t\t${flights.size}\t\t$minDirectCost\t\t$minNonDirectCost")
      flights
    }

  def retrieve(direction: CityDirection, dateFrom: LocalDate, dateTo: LocalDate, direct: Boolean): List[Flight] = {
    val f = (spApi ? GetTickets(direction, dateFrom, dateTo, direct, passengers)).mapTo[List[Flight]]

    val t = System.nanoTime()
    val result = Await.ready(f, Duration.Inf).value.get
    val time1 = System.nanoTime() - t
    time += time1
    n += 1

    log.warning(s"time: ${time1 / 1000 / 1000}\taverage: ${time / 1000 / 1000 / n}")

    result match {
      case Success(t) =>
        if (t.size == 200) {
          log.warning(s"${t.size}\t$direct\t$dateFrom\t$dateTo")
          val days = ChronoUnit.DAYS.between(dateFrom, dateTo) / 2
          val mid = dateFrom.plusDays(days)
          retrieve(direction, dateFrom, mid, direct) ++ retrieve(direction, mid.plusDays(1), dateTo, direct)
        } else {
          t
        }
      case Failure(e) => List()
    }
  }

  override def receive: Receive = {
    case x =>
      log.error(s"Received a message: $x")
  }
}
