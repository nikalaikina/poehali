package com.github.nikalaikina.poehali.sp

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.GetPlaces
import com.github.nikalaikina.poehali.model.City

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.ScalaCache
import scalacache.serialization.InMemoryRepr


class PlacesProvider(spApi: ActorRef)(implicit cache: ScalaCache[InMemoryRepr]) extends AbstractActor {

  import scalacache._

  override def receive: Receive = {
    case GetPlaces(n) =>
      cachedPlaces.map(l => l.take(n)) pipeTo sender()
  }

  def cachedPlaces: Future[List[City]] = {
    sync.cachingWithTTL("places")(24 hours) {
      (spApi ? GetPlaces).mapTo[List[City]]
    }
  }
}

object PlacesProvider {
  def props(spApi: ActorRef)(implicit cache: ScalaCache[InMemoryRepr]) = Props(new PlacesProvider(spApi))
}