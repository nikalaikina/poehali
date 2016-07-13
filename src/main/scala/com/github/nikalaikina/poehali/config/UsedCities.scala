package com.github.nikalaikina.poehali.config

import scala.io.Source

object UsedCities {

  val cities: List[String] = Source.fromFile("config/cities.txt").getLines.toList
}
