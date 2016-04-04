package com.github.nikalaikina

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.typesafe.config.Config

object Settings{
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  def getList(string: String) = string.split(",").toList.map(s => s.trim)
}

case class Settings(homeCities: List[String],
                    cities: List[String],
                    dateFrom: LocalDate,
                    dateTo: LocalDate,
                    daysFrom: Int,
                    daysTo: Int,
                    cost: Int,
                    citiesCount: Int) {


  def this(_homeCities: String,
           _cities: String,
           _dateFrom: String,
           _dateTo: String,
           _daysFrom: String,
           _daysTo: String,
           _cost: String,
           _citiesCount: String) {

    this (Settings.getList(_homeCities),
      Settings.getList(_homeCities) ++ Settings.getList(_cities),
          LocalDate.parse(_dateFrom, Settings.formatter),
          LocalDate.parse(_dateTo, Settings.formatter),
          _daysFrom.toInt,
          _daysTo.toInt,
          _cost.toInt,
          _citiesCount.toInt)
  }

  def this(config: Config) {
    this (homeCities = Settings.getList(config.getString ("homeCities")),
          cities = Settings.getList(config.getString("cities")) ++ Settings.getList(config.getString ("homeCities")),
          dateFrom = LocalDate.parse(config.getString("dateFrom"), Settings.formatter),
          dateTo = LocalDate.parse(config.getString("dateTo"), Settings.formatter),
          daysFrom = config.getInt("daysFrom"),
          daysTo = config.getInt("daysTo"),
          cost = config.getInt("cost"),
          citiesCount = config.getInt("citiesCount"))
  }

}