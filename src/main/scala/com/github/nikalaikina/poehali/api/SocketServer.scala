package com.github.nikalaikina.poehali.api



import java.net.InetSocketAddress

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.logic.{Cities, WsCalculator}
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
      case JsSuccess(value, path) =>
        map += webSocket -> WsCalculator.start(spApi, webSocket, value)
      case x =>
        println(x)
    }
  }

  override def onError(webSocket: WebSocket, exception: Exception) = {
    log.info(s"connection error $exception")
  }

}