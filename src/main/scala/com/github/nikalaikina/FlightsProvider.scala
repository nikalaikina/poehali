package com.github.nikalaikina

import java.sql.Timestamp
import java.time.{LocalDateTime, LocalDate}
import java.time.format.DateTimeFormatter

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import org.json4s._
import org.json4s.jackson.JsonMethods._

case class Flight(flyFrom: String, flyTo: String, price: Float, date: LocalDate)

private case class FlightJson(flyFrom: String, flyTo: String, price: Float, dTime: Long) {
  def date = new Timestamp(dTime * 1000).toLocalDateTime.toLocalDate
}

class FlightsProvider(val cities: List[String], val dateFrom: LocalDate, val dateTo: LocalDate) {
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  implicit val formats = DefaultFormats

  var map: Map[Tuple2[String, String], List[Flight]] = Map()

  for (c1 <- cities; c2 <- cities; if !(c1 == c2)) {
    val flights: List[Flight] = for (f <- getFlightsFromServer(c1, c2, dateFrom, dateTo))
      yield new Flight(f.flyFrom, f.flyTo, f.price, f.date)
    map += (Tuple2(c1, c2) -> flights)
  }
  println("Got flights info")

  private def getFlightsFromServer(from: String, to: String, dateFrom: LocalDate, dateTo: LocalDate) = {
    val urlPattern = s"https://api.skypicker.com/flights?flyFrom=$from&to=$to&dateFrom=${formatter.format(dateFrom)}&dateTo=${formatter.format(dateTo)}&directFlight=1"
    parseFlights(MyHttp.get(urlPattern))
  }

  def getFlights(from: String, to: String, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    val flights: Option[List[Flight]] = map.get(Tuple2(from, to))
    if (flights.isEmpty) List()
    else flights.get.filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo))
  }

  private def parseFlights(string: String): List[FlightJson] = {
    val elements: JValue = parse(string) \ "data"
    for (e <- elements.children) yield e.extract[FlightJson]
  }
}

private object MyHttp {
  def get(url: String,
          connectionTimeout: Int = 10000,
          socketTimeout: Int = 10000): String = {
    val httpClient = buildHttpClient(connectionTimeout, socketTimeout)
    val httpResponse = httpClient.execute(new HttpGet(url))
    val entity = httpResponse.getEntity
    var content = ""
    if (entity != null) {
      val inputStream = entity.getContent
      content = io.Source.fromInputStream(inputStream).getLines.mkString
      inputStream.close()
    }
    httpClient.getConnectionManager.shutdown()
    content
  }

  private def buildHttpClient(connectionTimeout: Int, socketTimeout: Int): DefaultHttpClient = {
    val httpClient = new DefaultHttpClient
    val httpParams = httpClient.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
    HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)
    httpClient.setParams(httpParams)
    httpClient
  }
}



