package com.github.nikalaikina.poehali.sp

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.pattern.pipe
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.{GetFlights, GetPlaces}
import com.github.nikalaikina.poehali.model._
import info.mukel.telegrambot4s.models.Location
import org.json4s.JsonAST.JValue
import org.json4s._
import org.json4s.jackson.JsonMethods._
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}

import scala.concurrent.Future

private case class SpFlight(flyFrom: String, flyTo: String, price: Float, dTimeUTC: Long, aTimeUTC: Long, route: List[SpRoute], booking_token: String) {
  def date = new Timestamp(dTimeUTC * 1000).toLocalDateTime.toLocalDate

  def toFlight = {
    val routes = List(route.head.toRoute)
    Flight(Direction(AirportId(flyFrom), AirportId(flyTo)), price, date: LocalDate, dTimeUTC, aTimeUTC, routes, s"https://www.kiwi.com/ru/booking?passengers=1&token=$booking_token")
  }
}

class SpApi extends AbstractActor {

  implicit val formats = DefaultFormats

  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val url = "https://api.skypicker.com"

  def flights(direction: CityDirection, dateFrom: LocalDate, dateTo: LocalDate): Future[List[Flight]] = {
    val urlPattern = s"$url/flights?flyFrom=${direction.from}" +
                                 s"&to=${direction.to}" +
                                 s"&dateFrom=${formatter.format(dateFrom)}" +
                                 s"&dateTo=${formatter.format(dateTo)}" +
                                 s"&directFlights=1"
    val future: Future[List[Flight]] = get(urlPattern)
      .map(parseFlights)
      .map(list => list.map(_.toFlight))
    future
  }


  private def parseFlights(string: String): List[SpFlight] = {
    val elements: JValue = parse(string) \ "data"
    elements.children.map(_.extract[SpFlight])
  }

  private def parseCities(string: String): List[SpCity] = {
    val elements: JValue = parse(string)
    elements.children.map(_.extract[SpCity])
  }

  def places(): Future[List[Airport]] = {
    val urlPattern = s"$url/places"
    get(urlPattern)
      .map(parseCities)
      .map(list => list.filter(c => c.lat.isDefined && c.lng.isDefined)
        .map(_.toCity)
        .sortBy(- _.score))
  }

  def get(url: String): Future[String] = {
    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    pipeline(Get(url)) map (_.entity.asString)
  }

  override def receive: Receive = {
    case GetPlaces => places() pipeTo sender()
    case GetFlights(direction, dateFrom, dateTo) => flights(direction, dateFrom, dateTo) pipeTo sender()
  }
}

case class SpCity(id: String, value: String, sp_score: Option[Int], lng: Option[Double], lat: Option[Double]) {
  def toCity = Airport(AirportId(id), value, sp_score.getOrElse(0), Location(lng.get, lat.get))
}

case class SpRoute(flight_no: Long, airline: String) {
  def toRoute = Route(flight_no, airline)
}
