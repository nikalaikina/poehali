package com.github.nikalaikina.poehali.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.typesafe.config.Config

case class Trip(homeCities: Set[String],
                cities: Set[String],
                dateFrom: LocalDate,
                dateTo: LocalDate,
                daysFrom: Int,
                daysTo: Int,
                passengers: Int = 1) {


  def this(_homeCities: String,
           _cities: String,
           _dateFrom: String,
           _dateTo: String,
           _daysFrom: Int,
           _daysTo: Int,
           _passengers: Int) {

    this (Trip.getSet(_homeCities),
          Trip.getSet(_homeCities) ++ Trip.getSet(_cities),
          LocalDate.parse(_dateFrom, Trip.formatter),
          LocalDate.parse(_dateTo, Trip.formatter),
          _daysFrom,
          _daysTo,
          _passengers)
  }

  def this(config: Config) {
    this (homeCities = Trip.getSet(config.getString ("homeCities")),
          cities = Trip.getSet(config.getString("cities")) ++ Trip.getSet(config.getString ("homeCities")),
          dateFrom = LocalDate.parse(config.getString("dateFrom"), Trip.formatter),
          dateTo = LocalDate.parse(config.getString("dateTo"), Trip.formatter),
          daysFrom = config.getInt("daysFrom"),
          daysTo = config.getInt("daysTo"))
  }

  val allCities = homeCities ++ cities
}

object Trip{
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  def getSet(string: String) = string.split(",").toList.map(s => s.trim).toSet
}