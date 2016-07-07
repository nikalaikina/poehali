package com.github.nikalaikina.poehali.bot

import akka.actor.Actor
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.logic.TripRoute
import com.github.nikalaikina.poehali.sp.FlightsProvider
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.SendMessage

import scala.io.Source

class PoehaliBot(fp: FlightsProvider) extends Actor with AbstractBot with AskSupport {

  def sendResult(person: Long, routes: List[TripRoute]): Unit = {
    val chat = new Chat
    val byCitiesCount: Map[Int, List[TripRoute]] = routes.groupBy(_.citiesCount(chat.homeCities))
    byCitiesCount.keys.toList.sortBy(-_).take(3).foreach(n => {
      byCitiesCount(n).groupBy(_.cities(chat.homeCities)) map {
        case (_, routes: List[TripRoute]) =>
          api.request(SendMessage( Left(person), routes.minBy(_.cost).toString))
        }
    })
  }

  override def receive: Receive = {
    case SendTextAnswer(id, text) => api.request(SendMessage( Left(id), text))
  }
}

case class SendTextAnswer(id: Long, text: String)

trait AbstractBot extends TelegramBot with Polling with Commands {
  def token = Source.fromFile("config/bot.token").getLines().next
}

class Chat {
  var homeCities = List()
  var cities = List()
}
