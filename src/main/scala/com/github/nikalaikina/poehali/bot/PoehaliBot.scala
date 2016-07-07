package com.github.nikalaikina.poehali.bot

import java.time.LocalDate

import akka.actor.Actor
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.api.Settings
import com.github.nikalaikina.poehali.logic.{Flight, Logic, TripRoute}
import com.github.nikalaikina.poehali.mesagge.Routes
import com.github.nikalaikina.poehali.sp.FlightsProvider
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models.{InlineKeyboardMarkup, Message}

import scala.io.Source

class PoehaliBot(fp: FlightsProvider) extends Actor with AbstractBot with AskSupport {

  on("/hello") { implicit msg => _ =>

    val markup = InlineKeyboardMarkup(Seq(Seq(), Seq()))

    api.request(SendMessage( Left(msg.sender), "ololol", replyMarkup = Option(markup)))
    println("wow msg")
  }

  def calc(msg: Message, chat: Chat): Unit = {
    val settings = Settings(chat.homeCities, chat.cities, LocalDate.now(), LocalDate.now().plusMonths(8), 4, 30, 600, Math.min(2, chat.cities.size - 3))
    (Logic.logic(settings, fp) ? Logic.GetRoutees)
      .mapTo[Routes]
      .map(r => sendResult(msg.sender, r.routes))
  }


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
