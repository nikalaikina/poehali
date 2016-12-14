package com.github.nikalaikina.poehali.logic

import akka.actor.{ActorContext, ActorRef, Props}
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.dao.{DirectionsToUpdate, DirectionsToUpdateResponse}
import com.github.nikalaikina.poehali.external.sp.TicketsProvider
import com.github.nikalaikina.poehali.model.CityDirection

import scalacache.ScalaCache
import scala.concurrent.duration._
import scala.util.Random


case class AutomaticCacheHeater(spApi: ActorRef, db: ActorRef)(implicit val cache: ScalaCache[Array[Byte]])
  extends AbstractActor with TicketsProvider {

  private implicit val cacheConfig = cache.cacheConfig

  context.system.scheduler.schedule(1 minutes, 10 minutes, self, AutomaticCacheHeater.Heat)


  override def receive: Receive = {
    case AutomaticCacheHeater.Heat =>
      db ! DirectionsToUpdate

    case DirectionsToUpdateResponse(directions) =>
      directions.foreach { d =>
        context.system.scheduler.scheduleOnce(Random.nextInt(550) + 1 seconds) {
          self ! AutomaticCacheHeater.HeatDirection(d)
        }
      }

    case AutomaticCacheHeater.HeatDirection(direction) =>
      putToCache(direction)
      putToCache(direction.inverse)
      log.info(s"${direction.from}\t\t${direction.to}")
  }

  def putToCache(d: CityDirection): Unit = {
    cache.cache.put(cache.keyBuilder.toCacheKey(Seq(d)), cc.serialize(retrieve(d)), Some(cacheTtl))
  }
}

object AutomaticCacheHeater {
  def start(spApi: ActorRef, db: ActorRef)(implicit citiesCache: ScalaCache[Array[Byte]], context: ActorContext) = {
    context.actorOf(Props(new AutomaticCacheHeater(spApi, db)))
  }

  case object Heat
  case class HeatDirection(direction: CityDirection)
}

