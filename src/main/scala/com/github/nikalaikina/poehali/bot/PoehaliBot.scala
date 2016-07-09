package com.github.nikalaikina.poehali.bot

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.logic.{Flight, TripRoute}
import com.github.nikalaikina.poehali.sp.{City, FlightsProvider}
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.SendMessage
import info.mukel.telegrambot4s.models._

import scala.collection.mutable

class PoehaliBot(fp: FlightsProvider, cities: Map[String, City]) extends Actor with AbstractBot with AskSupport {

  val formatter: Formatter = new Formatter(cities)
  val chats: mutable.Map[Long, ActorRef] = mutable.Map[Long, ActorRef]()

  override def preStart(): Unit = run()

  override def handleMessage(msg: Message): Unit = {
    super.handleMessage(msg)

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
    val chat = system.actorOf(Props(classOf[ChatFsm], fp, self, msg.sender))
    chats.put(msg.sender, chat)
  }

  on("/end") { implicit msg => _ =>
    chats(msg.sender) ! Calculate
  }

  override def receive: Receive = {
    case SendTextAnswer(id, text) =>
      api.request(SendMessage( Left(id), text))

    case SendBestRoutes(id, routes) =>
      api.request(SendMessage(Left(id), s"${formatter.getResult(id, routes)} \nGet details", replyMarkup = Option(detailsMarkup(routes.size, 5))))

    case SendRouteDetails(id, route) =>
      api.request(SendMessage(Left(id), s"Details: \n ${formatter.getDetails(id, route)}"))

    case SendCityRequest(id, except) =>
      val buttons: Seq[KeyboardButton] = cities.values
        .filter(c => !except.contains(c.id))
        .filter(c => UsedCities.cities.contains(c.id))
        .map(c => KeyboardButton(c.name))
        .toSeq

      api.request(SendMessage(Left(id), "Choose city", replyMarkup = Option(citiesMarkup(buttons, 3))))
  }

  def citiesMarkup(buttons: Seq[KeyboardButton], n: Int): ReplyKeyboardMarkup = {
    ReplyKeyboardMarkup(getMarkup(buttons, n) :+ Seq(KeyboardButton("/end")))
  }

  def detailsMarkup(buttons: Int, n: Int): ReplyKeyboardMarkup = {
    val buttons1 = (1 to buttons).map(i => KeyboardButton(i.toString))
    ReplyKeyboardMarkup(getMarkup(buttons1, n) :+ Seq(KeyboardButton("/start")))
  }

  def getMarkup(buttons: Seq[KeyboardButton], n: Int): Seq[Seq[KeyboardButton]] = {
    var seq2 = Seq[Seq[KeyboardButton]]()

    for (i <- 0 to buttons.size / n + 1) {
      seq2 = seq2 :+ buttons.slice(i * n, i * n + n)
    }
    seq2
  }
}

case class Formatter(cities: Map[String, City]) {

  def getDetails(person: Long, route: TripRoute): String = {
    route.flights.map(flightFormat).mkString("\n")
  }

  def getResult(person: Long, result: List[TripRoute]): String = {
    result.map(tripFormat).mkString("\n")
  }

  def flightFormat(flight: Flight): String = {
    s"[${flight.direction.from} -> ${flight.direction.to}\t${flight.date}\t${flight.price}]"
  }

  def tripFormat(route: TripRoute): String = {
    var citiesString = cities(route.flights.head.direction.from).name
    for (flight <- route.flights) {
      citiesString = citiesString + " -> " + cities(flight.direction.to).name
    }

    val cost = route.flights.map(_.price).sum

    s"[$citiesString]  ($cost$$)  ${route.firstDate} - ${route.curDate}"
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
