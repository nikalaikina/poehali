package com.github.nikalaikina.poehali.bot

import com.github.nikalaikina.poehali.logic.{Flight, TripRoute}

class Formatter(cities: Cities) {

  def getDetails(person: Long, route: TripRoute): String = {
    route.flights.map(flightFormat).mkString("\n")
  }

  def getResult(person: Long, result: List[TripRoute]): String = {
    val list = for ((route, i) <- result.zipWithIndex)
      yield s"`${i + 1}. `${tripFormat(route)}\n"
    list.mkString("\n")
  }

  def flightFormat(flight: Flight): String = {
    s"`${flight.direction.from.cityName} -> ${flight.direction.to.cityName}\t${flight.date}\t${flight.price}`"
  }

  def tripFormat(route: TripRoute): String = {
    val firstCity: String = route.flights.head.direction.from.cityName
    val citiesString = (firstCity :: route.flights.map(f => f.direction.to.cityName)).mkString(" -> ")

    s"*${route.cost}$$* *|* $citiesString *|* ${route.firstDate} - ${route.curDate}"
  }

  implicit class Imp(val s: String) {
    implicit def cityName: String = cities.cities(s).name
  }
}
