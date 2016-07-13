package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nikalaikina.poehali.api.RestInterface
import com.github.nikalaikina.poehali.bot.{Formatter, PoehaliBot}
import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.mesagge.GetPlaces
import com.github.nikalaikina.poehali.sp.{City, FlightsProvider, PlacesActor}
import spray.can.Http

import scala.collection.mutable
import scala.concurrent.duration._
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

object Boot extends App {
  val host = "localhost"
  val port = 8888

  implicit val system = ActorSystem("routes-service")
  implicit val executionContext = system.dispatcher

  implicit val cache = ScalaCache(GuavaCache())

  private val fp: FlightsProvider = new FlightsProvider()
  private val placesActor: ActorRef = system.actorOf(PlacesActor.props())
  val api = system.actorOf(Props(classOf[RestInterface], fp, placesActor), "httpInterface")

  runBot()

  implicit val timeout: Timeout = Timeout(1000 seconds)

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }

  def runBot(): Unit = {
    implicit val timeout: Timeout = Timeout(1000 seconds)
    (placesActor ? GetPlaces())
      .mapTo[List[City]]
      .map(list => {
        val fullMap: Map[String, City] = list.map(c => c.id -> c).toMap

        system.actorOf(Props(classOf[PoehaliBot], fp, fullMap), "bot")
      })

  }

}