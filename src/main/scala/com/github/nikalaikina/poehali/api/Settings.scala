package com.github.nikalaikina.poehali.api

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.typesafe.config.Config

case class Settings(homeCities: Set[String],
                    cities: Set[String],
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
           _daysFrom: Int,
           _daysTo: Int,
           _cost: Int,
           _citiesCount: Int) {

    this (Settings.getSet(_homeCities),
          Settings.getSet(_homeCities) ++ Settings.getSet(_cities),
          LocalDate.parse(_dateFrom, Settings.formatter),
          LocalDate.parse(_dateTo, Settings.formatter),
          _daysFrom,
          _daysTo,
          _cost,
          _citiesCount)
  }

  def this(config: Config) {
    this (homeCities = Settings.getSet(config.getString ("homeCities")),
          cities = Settings.getSet(config.getString("cities")) ++ Settings.getSet(config.getString ("homeCities")),
          dateFrom = LocalDate.parse(config.getString("dateFrom"), Settings.formatter),
          dateTo = LocalDate.parse(config.getString("dateTo"), Settings.formatter),
          daysFrom = config.getInt("daysFrom"),
          daysTo = config.getInt("daysTo"),
          cost = config.getInt("cost"),
          citiesCount = config.getInt("citiesCount"))
  }
}

object Settings{
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  def getSet(string: String) = string.split(",").toList.map(s => s.trim).toSet
}