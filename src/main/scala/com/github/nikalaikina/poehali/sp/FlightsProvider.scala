package com.github.nikalaikina.poehali.sp

import java.time.LocalDate

import com.github.nikalaikina.poehali.logic.Flight

import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.InMemoryRepr
import scalacache.{ScalaCache, sync}

case class Direction(from: String, to: String)

class FlightsProvider(implicit cache: ScalaCache[InMemoryRepr]) {

  def getFlights(direction: Direction, dateFrom: LocalDate, dateTo: LocalDate): List[Flight] = {
    val flights: List[Flight] = getFlightsCached(direction)
    if (flights.isEmpty) List()
    else flights.filter(f => !f.date.isBefore(dateFrom) && !f.date.isAfter(dateTo))
  }

  def getFlightsCached(direction: Direction): List[Flight] =
    sync.cachingWithTTL(direction)(24 hours) {
      val dateFrom = LocalDate.now()
      val dateTo = dateFrom.plusYears(1)
      SpApi.flights(direction, dateFrom, dateTo)
    }
}



