package com.github.nikalaikina.poehali.logic

import java.time.LocalDate
import java.time.temporal.TemporalAmount

import akka.actor.{ActorContext, ActorRef, Props}
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.logic.ManualCacheHeater.Heat
import com.github.nikalaikina.poehali.model.CityDirection
import com.github.nikalaikina.poehali.external.sp.TicketsProvider

import scalacache.ScalaCache


case class ManualCacheHeater(spApi: ActorRef)(implicit val citiesCache: ScalaCache[Array[Byte]])
  extends AbstractActor with TicketsProvider {


  override def receive: Receive = {
    case Heat(cities, time) =>
      val t = System.nanoTime()
      for (from <- cities; to <- cities if from != to) {
        val t = System.nanoTime()
        getFlightsCached(CityDirection(from, to))
        log.warning(s"${(System.nanoTime() - t) / 1000 / 1000} sec\t\tfor $from $to")
      }
      log.warning(s"Cache heated in ${(System.nanoTime() - t) / 1000 / 1000 / 1000} for ${cities.size} cities")
  }

}

object ManualCacheHeater {
  def start(spApi: ActorRef)(implicit citiesCache: ScalaCache[Array[Byte]], context: ActorContext) = {
    context.actorOf(Props(new ManualCacheHeater(spApi)))
  }

  case class Heat(cities: List[String], time: TemporalAmount)
}