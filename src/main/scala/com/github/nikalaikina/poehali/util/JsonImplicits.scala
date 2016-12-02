package com.github.nikalaikina.poehali.util

import com.github.nikalaikina.poehali.model._
import com.github.nikalaikina.poehali.to.JsonRoute
import info.mukel.telegrambot4s.models.Location
import play.api.libs.json.Json


object JsonImplicits {

  implicit val airportIdFormat = Json.format[AirportId]
  implicit val directionFormat = Json.format[Direction]
  implicit val cityDirectionFormat = Json.format[CityDirection]
  implicit val routeFormat = Json.format[Route]
  implicit val flightFormat = Json.format[Flight]
  implicit val jsonRouteFormat = Json.format[JsonRoute]
  implicit val locationFormat = Json.format[Location]
  implicit val cityFormat = Json.format[Airport]
  implicit val tripFormat = Json.format[Trip]

}
