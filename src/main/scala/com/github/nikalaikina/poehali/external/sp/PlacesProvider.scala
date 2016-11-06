package com.github.nikalaikina.poehali.external.sp

import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.logic.Cities
import com.github.nikalaikina.poehali.message.{GetCities, GetPlaces}
import com.github.nikalaikina.poehali.model.{Airport, Flight}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scalacache.ScalaCache
import scalacache.serialization.{GZippingBinaryCodec, InMemoryRepr}


class PlacesProvider(spApi: ActorRef)(implicit cache: ScalaCache[Array[Byte]]) extends AbstractActor {

  import scalacache._
  implicit val flags = Flags.defaultFlags
  implicit val cc: GZippingBinaryCodec[List[Airport]] = scalacache.serialization.GZippingJavaAnyBinaryCodec.default[List[Airport]]


  override def receive: Receive = {
    case GetPlaces(n) =>
      sender() ! cachedPlaces.take(n)
    case GetCities =>
      sender() ! Cities(cachedPlaces)
  }

  def cachedPlaces: List[Airport] = {
    sync.cachingWithTTL("places")(24 hours) {
      val f = (spApi ? GetPlaces).mapTo[List[Airport]]

      val result = Await.ready(f, Duration.Inf).value.get

      result match {
        case Success(t) => t
        case Failure(e) => List()
      }
    }
  }
}

object PlacesProvider {
  def props(spApi: ActorRef)(implicit cache: ScalaCache[Array[Byte]]) = Props(new PlacesProvider(spApi))
}