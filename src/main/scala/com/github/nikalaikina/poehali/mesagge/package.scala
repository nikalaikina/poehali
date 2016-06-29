package com.github.nikalaikina.poehali

import com.github.nikalaikina.poehali.logic.TripRoute


package object mesagge {

  sealed trait Request
  class GetRoutes extends Request
  sealed trait Response
  case class Routes(routes: List[TripRoute]) extends Response

}
