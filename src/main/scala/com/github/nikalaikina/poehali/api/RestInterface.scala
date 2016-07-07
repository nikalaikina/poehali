package com.github.nikalaikina.poehali.api

import akka.actor.{ActorContext, Actor, ActorSystem, Props}
import akka.pattern.AskSupport
import akka.util.Timeout
import com.github.nikalaikina.poehali.logic.{Flight, Logic}
import com.github.nikalaikina.poehali.mesagge.{GetRoutes, Routes}
import com.github.nikalaikina.poehali.sp.{City, SpApi, Direction, FlightsProvider}
import play.api.libs.json.Json
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class RestInterface(fp: FlightsProvider) extends HttpServiceActor with AskSupport with RestApi {
  implicit val system = context.system
  override def flightsProvider = fp
  def receive = runRoute(routes)
}

case class ApiFlight(flyFrom: String, flyTo: String, price: Float, timeFrom: Long, timeTo: Long)

trait RestApi extends HttpService { actor: Actor with AskSupport =>

  implicit val system: ActorSystem

  def flightsProvider: FlightsProvider

  case class JsonRoute(flights: List[Flight])

  import play.api.libs.json._
  implicit val timeout = Timeout(10 seconds)
  implicit val directionFormat = Json.format[Direction]
  implicit val flightFormat = Json.format[Flight]
  implicit val jsonRouteFormat = Json.format[JsonRoute]
  implicit val jsonCityFormat = Json.format[City]

  def routes: Route =
    pathPrefix("flights") {
      pathEnd {
        get {
          parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'cost.as[Int], 'citiesCount.as[Int])
          { (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount) => (ctx: RequestContext) =>
            val settings = Settings(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount)
            (Logic.logic(settings, flightsProvider) ? Logic.GetRoutees)
              .mapTo[Routes].map(r => r.routes.map(tr => new JsonRoute(tr.flights))).map { x =>
              ctx.complete(Json.toJson(x).toString)
            }

          }
        }
      }
    } ~
    pathPrefix("status") {
      pathEnd {
        get {
          complete("ok")
        }
      }
    } ~
    pathPrefix("cities") {
      pathEnd {
        get {
          parameters('number.as[Int]) { (number) => (ctx: RequestContext) =>
            ctx.complete(Json.toJson(SpApi.places(number)).toString())
          }
        }
      }
    }



}