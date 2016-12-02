package com.github.nikalaikina.poehali.test

import java.net.{InetSocketAddress, URI}

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.logic.WsCalculator
import com.github.nikalaikina.poehali.model.Trip
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.{ClientHandshake, ServerHandshake}
import org.java_websocket.server.WebSocketServer
import play.api.libs.json.{JsSuccess, Json}

import scalacache.ScalaCache


object CalculatorTest extends App {


  private val thread = new Thread(new Runnable {
    override def run(): Unit = {
      (1 to 5).map(_ => client).par.foreach(_.connect())
    }
  })
  thread.setDaemon(false)
  thread.start()

  Thread.sleep(2 * 60 * 1000)

  def client = new WebSocketClient(new URI("http://localhost:4242")) {

    val cities = UsedCities.cities.filterNot(_ == "Vilnius").toList

    def textMessage = {
      val citiesString = util.Random
        .shuffle(cities)
        .take(4)
          .map(_.replaceAll("%20", " "))
        .map(""""""" + _ + """"""")
        .mkString(",")

      s"""
         |{"homeCities":["Vilnius"],"cities":[$citiesString],"dateFrom":"2016-11-01","dateTo":"2017-03-30","daysFrom":4,"daysTo":20,"passengers":1}
      """.stripMargin
    }

    var t = System.nanoTime()

    override def onError(ex: Exception): Unit = {
      ex.printStackTrace()
    }

    override def onMessage(message: String): Unit = {

    }

    override def onClose(code: Int, reason: String, remote: Boolean): Unit = {
      val time = (System.nanoTime() - t) / 1000 / 1000 / 1000
      println("[=] " * 40)
      println(s"DONE in $time seconds")
    }

    override def onOpen(handshakedata: ServerHandshake): Unit = {
      t = System.nanoTime()
      println(handshakedata)
      send(textMessage)
    }
  }

}
