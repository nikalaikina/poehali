package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nikalaikina.poehali.api.RestInterface
import com.github.nikalaikina.poehali.bot.{Cities, PoehaliBot}
import com.github.nikalaikina.poehali.message.GetPlaces
import com.github.nikalaikina.poehali.model.City
import com.github.nikalaikina.poehali.sp.{FlightsProvider, PlacesProvider, SpApi}
import spray.can.Http

import scala.concurrent.Future
import scala.concurrent.duration._
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

object Boot extends App {
  val host = "0.0.0.0"
  val port = 8888

  import com.github.nikalaikina.poehali.util.TimeoutImplicits.waitForever

  implicit val system = ActorSystem("routes-service")
  implicit val executionContext = system.dispatcher
  implicit val cache = ScalaCache(GuavaCache())

  val spApi: ActorRef = system.actorOf(Props(classOf[SpApi]), "spApi")
  system.actorOf(FlightsProvider.props(spApi), "flightsProvider")
  val placesActor: ActorRef = system.actorOf(PlacesProvider.props(spApi), "placesProvider")

  val api = system.actorOf(Props(classOf[RestInterface], placesActor), "httpInterface")
  runBot()

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

  def runBot(): Unit = {
    (placesActor ? GetPlaces())
      .mapTo[List[City]]
      .onSuccess {
        case list: List[City] =>
          val fullMap: Map[String, City] = list.map(c => c.id -> c).toMap
          system.actorOf(Props(classOf[PoehaliBot], Cities(fullMap)), "bot")
        case _ => system.log.error("Bot hasn't been started")
      }
  }

}