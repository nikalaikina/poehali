package com.github.nikalaikina.poehali.config

import com.github.nikalaikina.poehali.model.AirportId

import scala.io.Source

object UsedCities {

  val cities: Set[String] = Source.fromFile("config/cities.txt").getLines.toList.toSet
}
