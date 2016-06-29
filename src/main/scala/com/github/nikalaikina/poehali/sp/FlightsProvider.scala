package com.github.nikalaikina.poehali.sp

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.github.nikalaikina.poehali.http.Http
import com.github.nikalaikina.poehali.logic.Flight
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.InMemoryRepr
import scalacache.{ScalaCache, sync}

case class Direction(from: String, to: String)

private case class SpFlight(flyFrom: String, flyTo: String, price: Float, dTimeUTC: Long, aTimeUTC: Long) {
  def date = new Timestamp(dTimeUTC * 1000).toLocalDateTime.toLocalDate
}

class FlightsProvider(implicit cache: ScalaCache[InMemoryRepr]) {
  implicit val formats = DefaultFormats
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  val spUrl = "https://api.skypicker.com"

  def getFlights(direction: Direction, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    val flights: List[Flight] = getFlightsCached(direction)
    if (flights.isEmpty) List()
    else flights.filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo))
  }

  def getFlightsCached(direction: Direction): List[Flight] =
    sync.cachingWithTTL(direction)(24 hours) {
      val dateFrom = LocalDate.now()
      val dateTo = dateFrom.plusMonths(8)
      val urlPattern = s"$spUrl/flights?flyFrom=${direction.from}" +
                                     s"&to=${direction.to}" +
                                     s"&dateFrom=${formatter.format(dateFrom)}" +
                                     s"&dateTo=${formatter.format(dateTo)}" +
                                     s"&directFlights=1"
      val json = Http.get(urlPattern)
      parseFlights(json).map(f => new Flight(Direction(f.flyFrom, f.flyTo), f.price, f.date, f.dTimeUTC, f.aTimeUTC))
    }

  private def parseFlights(string: String): List[SpFlight] = {
    val elements: JValue = parse(string) \ "data"
    elements.children.map(_.extract[SpFlight])
  }
}



