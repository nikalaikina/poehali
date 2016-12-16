package com.github.nikalaikina.poehali.logic

import akka.actor.Actor.Receive
import akka.actor.PoisonPill
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.model.TripRoute
import com.github.nikalaikina.poehali.to.JsonRoute
import org.java_websocket.WebSocket
import play.api.libs.json.Json

import scala.collection.mutable.ListBuffer

class WsBatchSendActor(socket: WebSocket) extends AbstractActor {

  import com.github.nikalaikina.poehali.util.JsonImplicits._

  val batchSize = 300

  var list: ListBuffer[TripRoute] = ListBuffer()

  override def receive: Receive = {
    case trip: TripRoute =>
      list += trip
      if (list.size == batchSize) {
        socket.send(Json.toJson(list.map(t => JsonRoute(t.flights))).toString())
        list = ListBuffer()
      }
    case CloseConnection =>
      if (!socket.isClosed) {
        socket.send(Json.toJson(list.map(t => JsonRoute(t.flights))).toString())
        socket.close()
      }
      self ! PoisonPill
  }
}

sealed trait WsBatchSendMessage

case object CloseConnection extends WsBatchSendMessage
