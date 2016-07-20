package com.github.nikalaikina.poehali.bot

import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.model.City
import com.github.nikalaikina.poehali.util.DistanceCalculator
import info.mukel.telegrambot4s.models.Location

case class Cities(cities: Map[String, City]) {

  def closest(location: Location, n: Integer, usedOnly: Boolean = false) = {
    cities
      .values
      .filter(c => !usedOnly || UsedCities.cities.contains(c.id))
      .toList
      .sortBy(c => DistanceCalculator.distance (location, c.location))
      .take(5)
  }

  def idByName(name: String): Option[String] = {
    UsedCities.cities
      .find(id => cities(id).name == name)
  }

  def except(ids: Set[String], usedOnly: Boolean = false) = {
    cities.values
      .filter(c => !ids.contains(c.id))
      .filter(c => !usedOnly || UsedCities.cities.contains(c.id))
  }
}
