package com.github.nikalaikina

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.typesafe.config.Config


class Settings(val config: Config) {
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val homeCities: List[String] = getList(config.getString("homeCities"))
  val cities: List[String] = homeCities ++ getList(config.getString("cities"))
  val dateFrom = LocalDate.parse(config.getString("dateFrom"), formatter)
  val dateTo = LocalDate.parse(config.getString("dateTo"), formatter)
  val daysFrom = config.getInt("daysFrom")
  val daysTo = config.getInt("daysTo")
  val cost = config.getInt("cost")
  val citiesCount = config.getInt("citiesCount")

  def getList(string: String) = string.split(",").toList.map(s => s.trim)
}