package com.github.nikalaikina.poehali.sp

import akka.actor.{Actor, Props}
import com.github.nikalaikina.poehali.mesagge.GetPlaces

import scala.language.postfixOps
import scalacache.serialization.InMemoryRepr
import scalacache.{ScalaCache, sync}
import scala.concurrent.duration._


class PlacesActor(implicit cache: ScalaCache[InMemoryRepr]) extends Actor {

  override def receive: Receive = {
    case GetPlaces(n) => sender() ! {
      sync.cachingWithTTL("places")(24 hours) {
        SpApi.places()
      }.take(n)
    }
  }
}

object PlacesActor {
  def props()(implicit cache: ScalaCache[InMemoryRepr]) = Props(new PlacesActor())
}