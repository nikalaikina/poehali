package com.github.nikalaikina.poehali.bot

import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.model.{Airport, AirportId}
import com.github.nikalaikina.poehali.util.DistanceCalculator
import info.mukel.telegrambot4s.models.Location

case class DefaultCities(cities: Map[AirportId, Airport]) {

  def closest(location: Location, n: Integer, usedOnly: Boolean = false) = {
    cities
      .values
      .filter(c => !usedOnly || UsedCities.cities.contains(c.city))
      .toList
      .sortBy(c => DistanceCalculator.distance (location, c.location))
      .take(5)
  }

  def except(cityNames: Set[String], usedOnly: Boolean = false) = {
    cities.values
      .filter(city => cityNames.contains(city.city))
      .filter(c => !usedOnly || UsedCities.cities.contains(c.city))
  }
}
