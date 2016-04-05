package com.github.nikalaikina.poehali.api

import akka.actor._
import akka.util.Timeout
import com.github.nikalaikina.poehali.logic.{Flight, Logic}
import play.api.libs.json.Json
import spray.routing._

import scala.concurrent.duration._
import scala.language.postfixOps

class RestInterface extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

case class ApiFlight(flyFrom: String, flyTo: String, price: Float, timeFrom: Long, timeTo: Long)

trait RestApi extends HttpService with ActorLogging { actor: Actor =>

  case class JsonRoute(flights: List[Flight])

  implicit val timeout = Timeout(10 seconds)
  implicit val flightFormat = Json.format[Flight]
  implicit val jsonRouteFormat = Json.format[JsonRoute]

  def routes: Route =
    pathPrefix("flights") {
      pathEnd {
        get {
          parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom, 'daysTo, 'cost, 'citiesCount)
            { (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount) =>
              val settings = new Settings(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount)
              val list: List[JsonRoute] = new Logic(settings).answer().map(tr => new JsonRoute(tr.flights))
              complete(Json.toJson(list).toString())
          }
        }
      } ~
      pathPrefix("status") {
        pathEnd {
          get {
              complete("ok")
            }
          }
        }
    }
}
