package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nikalaikina.poehali.api.RestInterface
import com.github.nikalaikina.poehali.bot.{Cities, PoehaliBot}
import com.github.nikalaikina.poehali.message.GetPlaces
import com.github.nikalaikina.poehali.sp.{City, FlightsProvider, PlacesProvider, SpApi}
import spray.can.Http

import scala.concurrent.Future
import scala.concurrent.duration._
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

object Boot extends App {
  val host = "localhost"
  val port = 8888

  implicit val system = ActorSystem("routes-service")
  implicit val executionContext = system.dispatcher

  implicit val cache = ScalaCache(GuavaCache())

  private val spApi: ActorRef = system.actorOf(Props(classOf[SpApi]), "spApi")
  system.actorOf(FlightsProvider.props(spApi), "flightsProvider")
  private val placesActor: ActorRef = system.actorOf(PlacesProvider.props(spApi), "placesProvider")
  val api = system.actorOf(Props(classOf[RestInterface], placesActor), "httpInterface")

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
    val future: Future[Any] = placesActor ? GetPlaces()
    val eventualCities: Future[List[City]] = future
      .mapTo[List[City]]
    eventualCities
      .map(list => {
        val fullMap: Map[String, City] = list.map(c => c.id -> c).toMap
        system.actorOf(Props(classOf[PoehaliBot], Cities(fullMap)), "bot")
      })

  }

}