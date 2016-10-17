package com.github.nikalaikina.poehali.message

import java.time.LocalDate

import com.github.nikalaikina.poehali.model.{CityDirection, Trip, TripRoute}

sealed trait Request
case class GetRoutees(trip: Trip) extends Request
case class GetFlights(direction: CityDirection, dateFrom: LocalDate, dateTo: LocalDate) extends Request
case class GetPlaces(number: Int = Integer.MAX_VALUE) extends Request
case object GetCities extends Request

sealed trait Response
case class Routes(routes: List[TripRoute]) extends Response