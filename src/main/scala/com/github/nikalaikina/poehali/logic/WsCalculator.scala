package com.github.nikalaikina.poehali.logic

import akka.actor._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.external.sp.TicketsProvider
import com.github.nikalaikina.poehali.to.JsonRoute
import org.java_websocket.WebSocket
import org.java_websocket.exceptions.WebsocketNotConnectedException
import play.api.libs.json.Json

import scala.language.postfixOps
import scalacache.ScalaCache

case class WsCalculator(spApi: ActorRef, socket: ActorRef, trip: Trip)(implicit val cache: ScalaCache[Array[Byte]])
  extends AbstractActor with TicketsProvider with Calculations {

  try {
    calc()
  } catch {
    case e: StopCalculationException =>
      log.debug("Client closed connection.")
  }
  socket ! CloseConnection
  context.stop(self)

  def addRoute(current: TripRoute): Boolean = {
    log.debug(s"Added route $current")
    socket ! current
    true
  }
}

object WsCalculator {
  def start(spApi: ActorRef, socket: ActorRef, trip: Trip)
           (implicit citiesCache: ScalaCache[Array[Byte]], context: ActorSystem) = {
    context.actorOf(Props(WsCalculator(spApi, socket, trip)))
  }
}