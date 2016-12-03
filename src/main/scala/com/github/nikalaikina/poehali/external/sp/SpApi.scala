package com.github.nikalaikina.poehali.external.sp

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.pattern.pipe
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.message.{GetTickets, GetPlaces}
import com.github.nikalaikina.poehali.model._
import info.mukel.telegrambot4s.models.Location
import org.json4s.JsonAST.JValue
import org.json4s._
import org.json4s.jackson.JsonMethods._
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}

import scala.concurrent.Future

private case class SpFlight(flyFrom: String, flyTo: String, cityFrom: String, cityTo: String, price: Float, dTimeUTC: Long, aTimeUTC: Long, route: List[SpRoute], booking_token: String, fly_duration: String) {
  def date = new Timestamp(dTimeUTC * 1000).toLocalDateTime.toLocalDate

  def toFlight = {
    val routes = route.map(_.toRoute)
    Flight(Direction(AirportId(flyFrom), AirportId(flyTo)),
      CityDirection(cityFrom, cityTo),
      price, date: LocalDate, dTimeUTC, aTimeUTC, routes,
      s"https://www.kiwi.com/ru/booking?price=$price&token=$booking_token", fly_duration)
  }
}

class SpApi extends AbstractActor {

  implicit val formats = DefaultFormats

  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val url = "https://api.skypicker.com"


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
    case GetPlaces =>
      places() pipeTo sender()
    case x: GetTickets =>
      import x._
      val urlPattern = s"$url/flights?flyFrom=${direction.from.replaceAll(" ", "%20")}" +
        s"&to=${direction.to.replaceAll(" ", "%20")}" +
        s"&dateFrom=${formatter.format(dateFrom)}" +
        s"&dateTo=${formatter.format(dateTo)}" +
        s"&passengers=$passengers" +
        s"&one_per_date=1" +
        s"&directFlights=${if (direct) 1 else 0}"
      get(urlPattern)
        .map(parseFlights)
        .map(_.map(_.toFlight)) pipeTo sender()
  }
}

case class SpCity(id: String, value: String, sp_score: Option[Int], lng: Option[Double], lat: Option[Double]) {
  def toCity = Airport(AirportId(id), value, sp_score.getOrElse(0), Location(lng.get, lat.get))
}

case class SpRoute(flight_no: Long, airline: String, lngFrom: Float, latFrom: Float, aTimeUTC: Long, dTimeUTC: Long, latTo: Float, lngTo: Float, flyFrom: String, flyTo: String, cityFrom: String, cityTo: String) {
  def toRoute = Route(flight_no, airline, lngFrom, latFrom, aTimeUTC, dTimeUTC, latTo, latFrom, flyFrom, flyTo, cityFrom, cityTo)
}
