package com.github.nikalaikina.poehali.sp

import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.github.nikalaikina.poehali.http.Http
import com.github.nikalaikina.poehali.logic.Flight
import org.json4s._
import org.json4s.jackson.JsonMethods._

private case class SpFlight(flyFrom: String, flyTo: String, price: Float, dTimeUTC: Long, aTimeUTC: Long) {
  def date = new Timestamp(dTimeUTC * 1000).toLocalDateTime.toLocalDate
}

class FlightsProvider(val cities: List[String], val dateFrom: LocalDate, val dateTo: LocalDate) {
  implicit val formats = DefaultFormats
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  val spUrl = "https://api.skypicker.com"

  var map: Map[Tuple2[String, String], List[Flight]] = Map()

  for (c1 <- cities; c2 <- cities; if !(c1 == c2)) {
    val flights: List[Flight] = for (f <- getFlightsFromSp(c1, c2, dateFrom, dateTo))
      yield new Flight(f.flyFrom, f.flyTo, f.price, f.date, f.dTimeUTC, f.aTimeUTC)
    map += (Tuple2(c1, c2) -> flights)
  }
  println("Got flights info")

  def getFlights(from: String, to: String, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    val flights: Option[List[Flight]] = map.get(Tuple2(from, to))
    if (flights.isEmpty) List()
    else flights.get.filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo))
  }

  private def getFlightsFromSp(from: String, to: String, dateFrom: LocalDate, dateTo: LocalDate) = {
    val urlPattern = s"$spUrl/flights?flyFrom=$from&to=$to&dateFrom=${formatter.format(dateFrom)}&dateTo=${formatter.format(dateTo)}&directFlights=1"
    val json: String = Http.get(urlPattern)
    parseFlights(json)
  }

  private def parseFlights(string: String): List[SpFlight] = {
    val elements: JValue = parse(string) \ "data"
    for (e <- elements.children) yield e.extract[SpFlight]
  }
}



