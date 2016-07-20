package com.github.nikalaikina.poehali.util

import com.github.nikalaikina.poehali.model.{City, Direction, Flight}
import com.github.nikalaikina.poehali.to.JsonRoute
import info.mukel.telegrambot4s.models.Location
import play.api.libs.json.Json


object JsonImplicits {

  implicit val directionFormat = Json.format[Direction]
  implicit val flightFormat = Json.format[Flight]
  implicit val jsonRouteFormat = Json.format[JsonRoute]
  implicit val jsonLocationFormat = Json.format[Location]
  implicit val jsonCityFormat = Json.format[City]

}
