package com.github.nikalaikina.poehali.config

import com.github.nikalaikina.poehali.model.AirportId

import scala.io.Source

object UsedCities {

  val cities: List[AirportId] = Source.fromFile("config/cities.txt").getLines.map(AirportId).toList
}
