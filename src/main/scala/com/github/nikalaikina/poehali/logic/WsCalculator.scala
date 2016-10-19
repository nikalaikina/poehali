package com.github.nikalaikina.poehali.logic

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.sp.FlightsProvider
import com.github.nikalaikina.poehali.to.JsonRoute
import org.java_websocket.WebSocket
import play.api.libs.json.Json

import scala.language.postfixOps
import scalacache.ScalaCache

case class WsCalculator(spApi: ActorRef, socket: WebSocket, trip: Trip)(implicit val citiesCache: ScalaCache[Array[Byte]])
  extends AbstractActor with FlightsProvider with Calculations {

  import com.github.nikalaikina.poehali.util.JsonImplicits._

  val precision = Math.min(5 - trip.cities.size, 1)
  var citiesCount = 1
  var cost = 2000f

  for (city <- trip.homeCities; day <- getFirstDays) {
    processNode(new TripRoute(city, day))
  }

  socket.close()
  context.stop(self)

  def addRoute(current: TripRoute): Unit = {
    log.debug(s"Added route $current")
    socket.send(Json.toJson(JsonRoute(current.flights)).toString())
    if (current.flights.size > citiesCount) {
      citiesCount = current.flights.size
      if (current.cost > cost) {
        cost = current.cost
      }
    }
    if (current.flights.size == citiesCount && current.cost < cost) {
      cost = current.cost
    }
  }
}

object WsCalculator {
  def start(spApi: ActorRef, socket: WebSocket, trip: Trip)
           (implicit citiesCache: ScalaCache[Array[Byte]], context: ActorSystem) = {
    context.actorOf(Props(WsCalculator(spApi, socket, trip)))
  }
}