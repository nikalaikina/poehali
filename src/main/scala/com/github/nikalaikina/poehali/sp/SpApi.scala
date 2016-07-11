package com.github.nikalaikina.poehali.sp

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.github.nikalaikina.poehali.logic.Flight
import com.github.nikalaikina.poehali.util.Http
import info.mukel.telegrambot4s.models.Location
import org.json4s._
import org.json4s.jackson.JsonMethods._

private case class SpFlight(flyFrom: String, flyTo: String, price: Float, dTimeUTC: Long, aTimeUTC: Long) {
  def date = new Timestamp(dTimeUTC * 1000).toLocalDateTime.toLocalDate
}

object SpApi {
  implicit val formats = DefaultFormats

  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val url = "https://api.skypicker.com"

  def flights(direction: Direction, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    val urlPattern = s"$url/flights?flyFrom=${direction.from}" +
                                 s"&to=${direction.to}" +
                                 s"&dateFrom=${formatter.format(dateFrom)}" +
                                 s"&dateTo=${formatter.format(dateTo)}" +
                                 s"&directFlights=1"
    parseFlights(Http.get2(urlPattern)).map(f => new Flight(Direction(f.flyFrom, f.flyTo), f.price, f.date, f.dTimeUTC, f.aTimeUTC))
  }


  private def parseFlights(string: String): List[SpFlight] = {
    val elements: JValue = parse(string) \ "data"
    elements.children.map(_.extract[SpFlight])
  }

  private def parseCities(string: String): List[SpCity] = {
    val elements: JValue = parse(string)
    elements.children.map(_.extract[SpCity])
  }

  def places(): List[City] = {
    val urlPattern = s"$url/places"
    parseCities(Http.get2(urlPattern))
      .filter(c => c.lat.isDefined && c.lng.isDefined)
      .map(c => new City(c.id, c.value, c.sp_score.getOrElse(0), Location(c.lng.get, c.lat.get))).sortBy(- _.score)
  }

  case class SpCity(id: String, value: String, sp_score: Option[Int], lng: Option[Double], lat: Option[Double])
}

case class City(id: String, name: String, score: Int, location: Location)