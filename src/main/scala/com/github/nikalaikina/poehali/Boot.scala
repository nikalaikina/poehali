package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import com.github.nikalaikina.poehali.api.RestInterface
import com.github.nikalaikina.poehali.bot.{DefaultCities, PoehaliBot}
import com.github.nikalaikina.poehali.message.GetPlaces
import com.github.nikalaikina.poehali.model.{Airport, AirportId}
import com.github.nikalaikina.poehali.sp.{PlacesProvider, SpApi}
import spray.can.Http

import scalacache.ScalaCache
import scalacache.guava.GuavaCache

object Boot extends App {
  val host = "0.0.0.0"
  val port = 8888

  import com.github.nikalaikina.poehali.util.TimeoutImplicits.waitForever

  implicit val system = ActorSystem("routes-service")
  implicit val executionContext = system.dispatcher
  implicit val citiesCache = ScalaCache(GuavaCache())

  val spApi: ActorRef = system.actorOf(Props(classOf[SpApi]), "spApi")
  val placesActor: ActorRef = system.actorOf(PlacesProvider.props(spApi), "placesProvider")

  (placesActor ? GetPlaces())
    .mapTo[List[Airport]]
    .onSuccess {
      case list: List[Airport] =>
        val fullMap: Map[AirportId, Airport] = list.map(c => c.id -> c).toMap
        system.actorOf(Props(classOf[PoehaliBot], DefaultCities(fullMap)), "bot")

        val api = system.actorOf(RestInterface.props(placesActor: ActorRef, spApi: ActorRef), "httpInterface")

        IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
          .mapTo[Http.Event]
          .map {
            case Http.Bound(address) =>
              system.log.debug(s"REST interface bound to $address")
            case Http.CommandFailed(cmd) =>
              system.log.error("REST interface could not bind to " +
                s"$host:$port, ${cmd.failureMessage}")
              system.terminate()
          }

      case _ => system.log.error("Bot hasn't been started")
    }

}