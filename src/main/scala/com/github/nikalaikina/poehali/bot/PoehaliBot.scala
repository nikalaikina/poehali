package com.github.nikalaikina.poehali.bot

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.sp.{City, FlightsProvider}
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, SendMessage}
import info.mukel.telegrambot4s.models._

import scala.collection.mutable

class PoehaliBot(fp: FlightsProvider, cities: mutable.Map[String, City]) extends Actor with AbstractBot with AskSupport {

  val chats: mutable.Map[Long, ActorRef] = mutable.Map[Long, ActorRef]()

  run()

  override def handleMessage(msg: Message): Unit = {
    super.handleMessage(msg)
    println(msg)

    if (!msg.text.get.startsWith("/")) {
      val option: Option[ActorRef] = chats.get(msg.sender)
      if (option.isDefined) {
        val cityId: String = cities.values.find(_.name == msg.text.get).get.id
        option.get ! AddCity(cityId)
      }
    }
  }

  on("/start") { implicit msg => _ =>
    val chat = system.actorOf(Props(classOf[ChatFsm], fp, self, msg.sender))
    chats.put(msg.sender, chat)
  }

  on("/end") { implicit msg => _ =>
    chats(msg.sender) ! Calculate
  }

  override def handleInlineQuery(inlineQuery: InlineQuery) = {

    val results = Seq[InlineQueryResult](
      InlineQueryResultArticle("article", "calc", InputTextMessageContent("ololol"))
    )

    api.request(AnswerInlineQuery(inlineQuery.id, results))
  }


  override def receive: Receive = {
    case SendTextAnswer(id, text) =>
      api.request(SendMessage( Left(id), text))

    case SendCityRequest(id: Long, except: List[String]) =>

      val buttons = cities.values
        .filter(c => !except.contains(c.id))
        .map(c => KeyboardButton(c.name))
        .toSeq

      var seq2 =  Seq[Seq[KeyboardButton]]()

      for (i <- 0 to buttons.size / 3 + 1) {
        seq2 = seq2 :+ buttons.slice(i * 3, i * 3 + 3)
      }

      seq2 = seq2 :+ Seq(KeyboardButton("/end"))
      api.request(SendMessage(Left(id), "Choose city", replyMarkup = Option(ReplyKeyboardMarkup(seq2))))
  }
}

class Chat {
  var homeCities = List[City]()
  var cities = List[City]()
}


import scala.io.Source

case class SendTextAnswer(id: Long, text: String)
case class SendCityRequest(id: Long, except: List[String])

trait AbstractBot extends TelegramBot with Polling with Commands {
  def token = Source.fromFile("config/bot.token").getLines().next
}
