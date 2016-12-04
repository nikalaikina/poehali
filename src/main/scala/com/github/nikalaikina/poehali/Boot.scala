package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import com.github.nikalaikina.poehali.api.{RestInterface, SocketServer}
import com.github.nikalaikina.poehali.bot.{DefaultCities, PoehaliBot}
import com.github.nikalaikina.poehali.dao.{DataAccessActor, DataAccessMessage}
import com.github.nikalaikina.poehali.logic.WsCalculator
import com.github.nikalaikina.poehali.model.{Airport, AirportId, Trip}
import com.github.nikalaikina.poehali.external.sp.{PlacesProvider, SpApi}
import play.api.libs.json.{JsSuccess, Json}
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

  val dbActor: ActorRef = system.actorOf(Props(classOf[DataAccessActor]), "dbActor")
  system.eventStream.subscribe(dbActor, classOf[DataAccessMessage])

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

  def testWs: Any = {
    import com.github.nikalaikina.poehali.util.JsonImplicits._

    val message =
      """
        |{"homeCities":["Vilnius"],"cities":["Brussels","Amsterdam"],"dateFrom":"2016-11-01","dateTo":"2017-03-30","daysFrom":4,"daysTo":16}
      """.stripMargin

    Json.fromJson[Trip](Json.parse(message)) match {
      case JsSuccess(value, path) =>
        WsCalculator.start(spApi, null, value)
      case x =>
        println(x)
    }
  }
}