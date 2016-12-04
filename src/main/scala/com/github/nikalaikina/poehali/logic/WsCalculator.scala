package com.github.nikalaikina.poehali.logic

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.external.sp.TicketsProvider
import com.github.nikalaikina.poehali.to.JsonRoute
import org.java_websocket.WebSocket
import play.api.libs.json.Json

import scala.language.postfixOps
import scalacache.ScalaCache

case class WsCalculator(spApi: ActorRef, socket: WebSocket, trip: Trip)(implicit val cache: ScalaCache[Array[Byte]])
  extends AbstractActor with TicketsProvider with Calculations {

  import com.github.nikalaikina.poehali.util.JsonImplicits._

  calc()
  socket.close()
  context.stop(self)

  def addRoute(current: TripRoute): Unit = {
    log.debug(s"Added route $current")
    socket.send(Json.toJson(JsonRoute(current.flights)).toString())
  }
}

object WsCalculator {
  def start(spApi: ActorRef, socket: WebSocket, trip: Trip)
           (implicit citiesCache: ScalaCache[Array[Byte]], context: ActorSystem) = {
    context.actorOf(Props(WsCalculator(spApi, socket, trip)))
  }
}