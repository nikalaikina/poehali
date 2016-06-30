package com.github.nikalaikina.poehali

import com.github.nikalaikina.poehali.logic.TripRoute


package object mesagge {

  sealed trait Request
  case object GetRoutees extends Request
  case class GetPlaces(number: Int) extends Request

  sealed trait Response
  case class Routes(routes: List[TripRoute]) extends Response

}
