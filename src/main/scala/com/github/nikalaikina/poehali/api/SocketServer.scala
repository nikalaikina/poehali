package com.github.nikalaikina.poehali.api



import java.net.InetSocketAddress

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.event.LoggingAdapter
import com.github.nikalaikina.poehali.dao.{CityUsed, CityUsedAsHome}
import com.github.nikalaikina.poehali.logic.{Cities, WsBatchSendActor, WsCalculator}
import com.github.nikalaikina.poehali.model.{Flight, Trip, TripRoute}
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import play.api.libs.json._

import scalacache.ScalaCache


case class SocketServer(host: String,
                        port: Int,
                        log: LoggingAdapter,
                        spApi: ActorRef)(implicit val citiesCache: ScalaCache[Array[Byte]], context: ActorSystem)
  extends WebSocketServer(new InetSocketAddress(host, port)) {

  var map: Map[WebSocket, ActorRef] = Map()

  import com.github.nikalaikina.poehali.util.JsonImplicits._

  override def onOpen(ws: WebSocket, clientHandshake: ClientHandshake) = {
    log.info(s"ws connection open")
  }

  override def onClose(webSocket: WebSocket, code: Int, reason: String, remote: Boolean) = {
    log.info(s"connection close reason: $reason")
    map -= webSocket
  }

  override def onMessage(webSocket: WebSocket, message: String) = {
    log.info(s"message given: $message")
    Json.fromJson[Trip](Json.parse(message)) match {
      case JsSuccess(trip, path) =>
        val batchSender = context.actorOf(Props(classOf[WsBatchSendActor], webSocket))

        map += webSocket -> WsCalculator.start(spApi, batchSender, trip)
        trip.cities.foreach(c => context.eventStream.publish(CityUsed(c)))
        trip.homeCities.foreach(c => context.eventStream.publish(CityUsedAsHome(c)))
      case x =>
        println(x)
    }
  }

  override def onError(webSocket: WebSocket, exception: Exception) = {
    log.info(s"connection error $exception")
  }

}