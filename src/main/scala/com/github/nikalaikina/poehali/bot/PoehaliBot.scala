package com.github.nikalaikina.poehali.bot

import java.lang.Math.{acos, cos, sin}

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.logic.{Flight, TripRoute}
import com.github.nikalaikina.poehali.sp.{City, FlightsProvider}
import info.mukel.telegrambot4s.api.{Commands, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}
import info.mukel.telegrambot4s.models._

import scala.collection.mutable

object MessagePatterns {
  val NumberPattern = "(\\d+)".r
  val CityPattern = "(^[A-Z][a-z]+)".r
}

class PoehaliBot(fp: FlightsProvider, cities: Map[String, City]) extends Actor with AbstractBot with AskSupport {

  import MessagePatterns._

  val formatter: Formatter = new Formatter(cities)
  val chats: mutable.Map[Long, ActorRef] = mutable.Map[Long, ActorRef]()

  override def preStart(): Unit = run()

  override def handleMessage(msg: Message): Unit = {
    implicit val m = msg

    msg.location match {
      case Some (location) =>
        val buttons = cities
          .values
          .filter(c => UsedCities.cities.contains(c.id))
          .toList
          .sortBy(c => DistanceCalculator.distance (location, c.location))
          .take(5)
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
          UsedCities.cities
            .find(id => cities(id).name == cityName)
            .foreach(id => getChat() ! AddCity(id))
        case x => super.handleMessage(msg)
      }
      case None =>
    }
  }

  on("/start") { implicit msg => _ =>
    chats.put(msg.sender, newChat())
    api.request(SendMessage(Left(msg.sender), "Send location:",
      replyMarkup = Option(ReplyKeyboardMarkup(Seq(Seq(KeyboardButton("/send", requestLocation = Some(true))))))))
  }

  on("/end") { implicit msg => _ =>
    getChat() ! Next
  }

  override def receive: Receive = {
    case SendTextAnswer(id, text) =>
      api.request(SendMessage( Left(id), text))

    case SendBestRoutes(id, routes) =>
      api.request(SendMessage(Left(id), s"${formatter.getResult(id, routes)} \n\nGet details:",
        replyMarkup = Option(detailsMarkup(routes.size, 5)) , parseMode = Some(ParseMode.Markdown)))

    case SendRouteDetails(id, route) =>
      api.request(SendMessage(Left(id), s"Details: \n${formatter.getDetails(id, route)}", Some(ParseMode.Markdown)))

    case SendCityRequest(id, except) =>
      val buttons: Seq[KeyboardButton] = cities.values
        .filter(c => !except.contains(c.id))
        .filter(c => UsedCities.cities.contains(c.id))
        .map(c => KeyboardButton(c.name))
        .toSeq

      api.request(SendMessage(Left(id), "Choose city:", replyMarkup = Option(citiesMarkup(buttons, 3))))
  }


  def getChat()(implicit msg: Message): ActorRef = {
    chats.getOrElseUpdate(msg.sender, newChat())
  }

  def newChat()(implicit msg: Message): ActorRef = {
    system.actorOf(Props(classOf[ChatFsm], fp, self, msg.sender))
  }

  def citiesMarkup(buttons: Seq[KeyboardButton], n: Int): ReplyKeyboardMarkup = {
    ReplyKeyboardMarkup(getMarkup(buttons, n) :+ Seq(KeyboardButton("/end")))
  }

  def detailsMarkup(buttons: Int, n: Int): ReplyKeyboardMarkup = {
    val buttons1 = (1 to buttons).map(i => KeyboardButton(i.toString))
    ReplyKeyboardMarkup(getMarkup(buttons1, n) :+ Seq(KeyboardButton("/start")))
  }

  def getMarkup(buttons: Seq[KeyboardButton], n: Int): Seq[Seq[KeyboardButton]] = {
    (0 to buttons.size / n + 1)
      .foldLeft(Seq[Seq[KeyboardButton]]())((seq, i) =>  seq :+ buttons.slice(i * n, i * n + n))
  }
}

case class Formatter(cities: Map[String, City]) {

  def getDetails(person: Long, route: TripRoute): String = {
    route.flights.map(flightFormat).mkString("\n")
  }

  def getResult(person: Long, result: List[TripRoute]): String = {
    val list = for ((route, i) <- result.zipWithIndex)
      yield s"`${i + 1}. `${tripFormat(route)}\n"
    list.mkString("\n")
  }

  def flightFormat(flight: Flight): String = {
    s"`${flight.direction.from.cityName} -> ${flight.direction.to.cityName}\t${flight.date}\t${flight.price}`"
  }

  def tripFormat(route: TripRoute): String = {
    val firstCity: String = route.flights.head.direction.from.cityName
    val citiesString = (firstCity :: route.flights.map(f => f.direction.to.cityName)).mkString(" -> ")

    s"*${route.cost}$$* *|* $citiesString *|* ${route.firstDate} - ${route.curDate}"
  }

  implicit class Imp(val s: String) {
    implicit def cityName: String = cities(s).name
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

object DistanceCalculator
{
  def distance(x: Location, y: Location) = {
    val theta = x.longitude - y.longitude
    val dist = sin(deg2rad(x.latitude)) * sin(deg2rad(y.latitude)) +
      cos(deg2rad(x.latitude)) * cos(deg2rad(y.latitude)) *
      cos(deg2rad(theta))
    rad2deg(acos(dist)) * 60 * 1.1515 * 1.609344
  }

  def deg2rad(deg: Double) = deg * Math.PI / 180.0
  def rad2deg(rad: Double) = rad * 180 / Math.PI
}