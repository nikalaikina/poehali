package com.github.nikalaikina.poehali.dao

import akka.pattern._
import com.github.nikalaikina.poehali.common.AbstractActor
import com.github.nikalaikina.poehali.model.CityDirection


class DataAccessActor extends AbstractActor {
  val dao = new Dao

  override def receive: Receive = {
    case DirectionsToUpdate(take) =>
      dao.directionsToUpdate(take) pipeTo sender()

    case CacheUpdated(direction) =>
      dao.cacheUpdated(direction)

    case CityUsed(city) =>
      dao.cityUsed(city)

    case CityUsedAsHome(city) =>
      dao.cityUsedAsHome(city)
  }
}

sealed trait DataAccessMessage
case class DirectionsToUpdate(take: Int = 5) extends DataAccessMessage
case class CacheUpdated(direction: CityDirection) extends DataAccessMessage
case class CityUsed(city: String) extends DataAccessMessage
case class CityUsedAsHome(city: String) extends DataAccessMessage

sealed trait DataAccessResponse
case class DirectionsToUpdateResponse(directions: Seq[CityDirection]) extends DataAccessResponse
