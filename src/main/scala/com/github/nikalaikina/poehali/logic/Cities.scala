package com.github.nikalaikina.poehali.logic

import com.github.nikalaikina.poehali.model.{Airport, AirportId}


case class Cities(list: List[Airport]) {

  val airports: Map[AirportId, Airport] = list.map(airport => airport.id -> airport).toMap

  val byName: Map[String, List[Airport]] = list.groupBy(_.city)

  val byAirport = list.map(airport => airport.id -> byName(airport. city)).toMap


}
