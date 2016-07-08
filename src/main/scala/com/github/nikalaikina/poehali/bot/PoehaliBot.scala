package com.github.nikalaikina.poehali.bot

import java.lang.Character

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.sp.{City, FlightsProvider}
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{AnswerInlineQuery, SendMessage}
import info.mukel.telegrambot4s.models._

import scala.collection.mutable

class PoehaliBot(fp: FlightsProvider, cities: mutable.Map[String, City], formatter: Formatter) extends Actor with AbstractBot with AskSupport {

  val chats: mutable.Map[Long, ActorRef] = mutable.Map[Long, ActorRef]()

  run()

  override def handleMessage(msg: Message): Unit = {
    super.handleMessage(msg)
    println(msg)

    if (msg.text.get.forall(_.isDigit)) {
      val option: Option[ActorRef] = chats.get(msg.sender)
      if (option.isDefined) {
        option.get ! GetDetails(msg.text.get.toInt)
      }
    }

    if (!msg.text.get.startsWith("/")) {
      val option: Option[ActorRef] = chats.get(msg.sender)
      if (option.isDefined) {
        val cityId: String = cities.values.find(_.name == msg.text.get).get.id
        option.get ! AddCity(cityId)
      }
    }
  }

  on("/start") { implicit msg => _ =>
    val chat = system.actorOf(Props(classOf[ChatFsm], fp, self, msg.sender, formatter))
    chats.put(msg.sender, chat)
  }

  on("/end") { implicit msg => _ =>
    chats(msg.sender) ! Calculate
  }

  override def receive: Receive = {
    case SendTextAnswer(id, text) =>
      api.request(SendMessage( Left(id), text))

    case SendDetailsRequest(id, n) =>
      api.request(SendMessage(Left(id), "Get details", replyMarkup = Option(detailsMarkup(n, 5))))

    case SendCityRequest(id, except) =>
      val buttons: Seq[KeyboardButton] = cities.values
        .filter(c => !except.contains(c.id))
        .map(c => KeyboardButton(c.name))
        .toSeq

      api.request(SendMessage(Left(id), "Choose city", replyMarkup = Option(citiesMarkup(buttons, 3))))
  }

  def citiesMarkup(buttons: Seq[KeyboardButton], n: Int): ReplyKeyboardMarkup = {
    ReplyKeyboardMarkup(getMarkup(buttons, n) :+ Seq(KeyboardButton("/end")))
  }

  def detailsMarkup(buttons: Int, n: Int): ReplyKeyboardMarkup = {
    val buttons1 = (1 to buttons).map(i => KeyboardButton(i.toString))
    ReplyKeyboardMarkup(getMarkup(buttons1, n) :+ Seq(KeyboardButton("/end")))
  }

  def getMarkup(buttons: Seq[KeyboardButton], n: Int): Seq[Seq[KeyboardButton]] = {
    var seq2 = Seq[Seq[KeyboardButton]]()

    for (i <- 0 to buttons.size / n + 1) {
      seq2 = seq2 :+ buttons.slice(i * n, i * n + n)
    }
    seq2
  }
}

import scala.io.Source

case class SendTextAnswer(id: Long, text: String)
case class SendCityRequest(id: Long, except: Set[String])
case class SendDetailsRequest(id: Long, k: Int)

trait AbstractBot extends TelegramBot with Polling with Commands {
  def token = Source.fromFile("config/bot.token").getLines().next
}
