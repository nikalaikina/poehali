package com.github.nikalaikina.poehali.util

import com.github.nikalaikina.poehali.api.JsonRoute
import com.github.nikalaikina.poehali.logic.Flight
import com.github.nikalaikina.poehali.sp.{City, Direction}
import info.mukel.telegrambot4s.models.Location
import play.api.libs.json.Json


object JsonImplicits {

  implicit val directionFormat = Json.format[Direction]
  implicit val flightFormat = Json.format[Flight]
  implicit val jsonRouteFormat = Json.format[JsonRoute]
  implicit val jsonLocationFormat = Json.format[Location]
  implicit val jsonCityFormat = Json.format[City]

}
