package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import com.github.nikalaikina.poehali.api.{RestInterface, SocketServer}
import com.github.nikalaikina.poehali.bot.{DefaultCities, PoehaliBot}
import com.github.nikalaikina.poehali.model.{Airport, AirportId}
import com.github.nikalaikina.poehali.sp.{PlacesProvider, SpApi}
import redis.clients.jedis.JedisPool
import spray.can.Http
import spray.can.Http.Event

import scala.concurrent.Future
import scalacache.ScalaCache
import scalacache.redis.RedisCache


object Boot extends App {
  val host = "0.0.0.0"
  val port = 8888

  import com.github.nikalaikina.poehali.util.TimeoutImplicits.waitForever

  implicit val system = ActorSystem("routes-service")
  implicit val executionContext = system.dispatcher

  implicit val scalaCache = getCache

  val spApi: ActorRef = system.actorOf(Props(classOf[SpApi]), "spApi")
  val placesActor: ActorRef = system.actorOf(PlacesProvider.props(spApi), "placesProvider")

  runSocketServer()
  runRestApi()

  def getCache: ScalaCache[Array[Byte]] = {
    val jedis = new JedisPool()
    val cache = new RedisCache(jedisPool = jedis, useLegacySerialization = true)
    ScalaCache(cache)
  }

  def runSocketServer(): Unit = {
    val socketServer = SocketServer("localhost", 4242, system.log, spApi)
    socketServer.start()
  }

  def runRestApi(): Future[Any] = {
    val api = system.actorOf(RestInterface.props(placesActor: ActorRef, spApi: ActorRef), "httpInterface")

    IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
      .mapTo[Event]
      .map {
        case Http.Bound(address) =>
          system.log.debug(s"REST interface bound to $address")
        case Http.CommandFailed(cmd) =>
          system.log.error("REST interface could not bind to " +
            s"$host:$port, ${cmd.failureMessage}")
          system.terminate()
      }
  }

  def runBot(list: List[Airport]): ActorRef = {
    val fullMap: Map[AirportId, Airport] = list.map(c => c.id -> c).toMap
    system.actorOf(Props(classOf[PoehaliBot], DefaultCities(fullMap)), "bot")
  }
}