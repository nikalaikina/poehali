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

  val toSendSize = 30

  var list: ListBuffer[TripRoute] = ListBuffer()

  var i = 0

  override def receive: Receive = {
    case trip: TripRoute =>
      list += trip
      i += 1
      if (i == batchSize) {
        socket.send(Json.toJson(batchToSend.map(t => JsonRoute(t.flights))).toString())
        i = 0
      }
    case CloseConnection =>
      if (!socket.isClosed) {
        socket.send(Json.toJson(batchToSend.map(t => JsonRoute(t.flights))).toString())
        socket.close()
      }
      self ! PoisonPill
  }

  def batchToSend: List[TripRoute] = {
    val grouped = list
      .groupBy(_.flights.size)
      .mapValues(_.sortBy(_.cost))
      .toList.sortBy(-_._1)
      .take(3)

    val groupSize = toSendSize / grouped.size
    val toSend = grouped.flatMap(_._2.take(groupSize))
    toSend
  }
}

sealed trait WsBatchSendMessage

case object CloseConnection extends WsBatchSendMessage
