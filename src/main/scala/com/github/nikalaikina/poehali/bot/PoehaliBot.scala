package com.github.nikalaikina.poehali.bot

import akka.actor.{ActorRef, Props}
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.model.TripRoute
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}
import info.mukel.telegrambot4s.models._

import scala.collection.mutable

object MessagePatterns {
  val NumberPattern = "(\\d+)".r
  val CityPattern = "(^[A-Z][a-z]+)".r
}

class PoehaliBot(cities: Cities) extends AbstractActor with AbstractBot  {

  override val log = super[AbstractActor].log

  import MessagePatterns._

  val formatter: Formatter = new Formatter(cities)
  val chats: mutable.Map[Long, ActorRef] = mutable.Map[Long, ActorRef]()

  override def preStart(): Unit = run()

  override def handleMessage(msg: Message): Unit = {
    implicit val m = msg

    msg.location match {
      case Some (location) =>
        val buttons = cities
          .closest(location, 5, usedOnly = true)
          .map(c => new KeyboardButton (c.name))
        api.request(SendMessage(chatId = Left(msg.sender),
                                text = "Choose home city:",
                                replyMarkup = Option(citiesMarkup(buttons, 3))))
      case None =>
    }

    msg.text match {
      case Some(text) => text match {
        case NumberPattern(n) =>
          getChat() ! GetDetails(n.toInt)
        case CityPattern(cityName) =>
          cities
            .idByName(cityName)
            .foreach(id => getChat() ! AddCity(id))
        case x => super.handleMessage(msg)
      }
      case None =>
    }
  }

  on("/start") { implicit msg => _ =>
    chats += msg.sender -> newChat()
    api.request(SendMessage(Left(msg.sender), "Send location:",
      replyMarkup = Option(ReplyKeyboardMarkup(Seq(Seq(KeyboardButton("/send", requestLocation = Some(true))))))))
  }

  on("/end") { implicit msg => _ =>
    getChat() ! Next
  }

  override def receive: Receive = {
    case SendTextAnswer(id, text) =>
      api.request(SendMessage(Left(id), text))

    case SendBestRoutes(id, routes) =>
      api.request(SendMessage(Left(id), s"${formatter.getResult(id, routes)} \n\nGet details:",
        replyMarkup = Option(detailsMarkup(routes.size, 5)) , parseMode = Some(ParseMode.Markdown)))

    case SendRouteDetails(id, route) =>
      api.request(SendMessage(Left(id), s"Details: \n${formatter.getDetails(id, route)}", Some(ParseMode.Markdown)))

    case SendCityRequest(id, except) =>
      val buttons: Seq[KeyboardButton] = cities
        .except(except, usedOnly = true)
        .map(c => KeyboardButton(c.name))
        .toSeq

      api.request(SendMessage(Left(id), "Choose city:", replyMarkup = Option(citiesMarkup(buttons, 3))))
  }


  def getChat()(implicit msg: Message): ActorRef = {
    chats.getOrElseUpdate(msg.sender, newChat())
  }

  def newChat()(implicit msg: Message): ActorRef = {
    context.actorOf(Props(classOf[ChatFsm], self, msg.sender))
  }

  def citiesMarkup(buttons: Seq[KeyboardButton], n: Int): ReplyKeyboardMarkup = {
    ReplyKeyboardMarkup(getMarkup(buttons, n) :+ Seq(KeyboardButton("/end")))
  }

  def detailsMarkup(number: Int, rowLength: Int): ReplyKeyboardMarkup = {
    ReplyKeyboardMarkup(getMarkup((1 to number).map(i => KeyboardButton(i.toString)), rowLength) :+ Seq(KeyboardButton("/start")))
  }

  def getMarkup(buttons: Seq[KeyboardButton], rowLength: Int): Seq[Seq[KeyboardButton]] = {
    def row(i: Int) = buttons.slice(i * rowLength, i * rowLength + rowLength)
    (0 to buttons.size / rowLength + 1)
      .foldLeft(Seq[Seq[KeyboardButton]]())((seq, i) =>  seq :+ row(i))
  }
}

import scala.io.Source

case class SendTextAnswer(id: Long, text: String)
case class SendCityRequest(id: Long, except: Set[String])
case class SendDetailsRequest(id: Long, k: Int)
case class SendBestRoutes(id: Long, routes: List[TripRoute])
case class SendRouteDetails(id: Long, route: TripRoute)

trait AbstractBot extends TelegramBot with Polling with Commands {
  def token = Source.fromFile("config/bot.token").getLines().next
}