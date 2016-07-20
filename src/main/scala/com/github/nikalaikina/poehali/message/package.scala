package com.github.nikalaikina.poehali

import java.time.LocalDate

import com.github.nikalaikina.poehali.model.{Direction, TripRoute}


package object message {

  sealed trait Request
  case object GetRoutees extends Request
  case class GetFlights(direction: Direction, dateFrom: LocalDate, dateTo: LocalDate) extends Request
  case class GetPlaces(number: Int = Integer.MAX_VALUE) extends Request

  sealed trait Response
  case class Routes(routes: List[TripRoute]) extends Response
}
