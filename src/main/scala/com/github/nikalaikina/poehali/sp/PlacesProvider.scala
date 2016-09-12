package com.github.nikalaikina.poehali.sp

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.logic.Cities
import com.github.nikalaikina.poehali.message.{GetCities, GetPlaces}
import com.github.nikalaikina.poehali.model.Airport

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
    case GetCities =>
      cachedPlaces.map(Cities) pipeTo sender()
  }

  def cachedPlaces: Future[List[Airport]] = {
    sync.cachingWithTTL("places")(24 hours) {
      (spApi ? GetPlaces).mapTo[List[Airport]]
    }
  }
}

object PlacesProvider {
  def props(spApi: ActorRef)(implicit cache: ScalaCache[InMemoryRepr]) = Props(new PlacesProvider(spApi))
}