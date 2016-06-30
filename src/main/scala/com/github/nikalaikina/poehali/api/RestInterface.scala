package com.github.nikalaikina.poehali.api

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.AskSupport
import akka.util.Timeout
import com.github.nikalaikina.poehali.logic.{Flight, Logic}
import com.github.nikalaikina.poehali.mesagge.Routes
import com.github.nikalaikina.poehali.sp.{Direction, FlightsProvider}
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

  implicit val timeout = Timeout(10 seconds)
  implicit val directionFormat = Json.format[Direction]
  implicit val flightFormat = Json.format[Flight]
  implicit val jsonRouteFormat = Json.format[JsonRoute]

  def routes: Route =
    pathPrefix("flights") {
      pathEnd {
        get {
          parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'cost.as[Int], 'citiesCount.as[Int])
          { (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount) => (ctx: RequestContext) =>
            val settings = new Settings(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount)
            (logic(settings) ? Logic.GetRoutees)
              .mapTo[Routes].map(r => r.routes.map(tr => new JsonRoute(tr.flights))).map { x =>
              println("@"*100)
              ctx.complete(Json.toJson(x).toString)
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
        }
    }


  def logic(settings: Settings) = context.actorOf(Props(classOf[Logic], settings, flightsProvider))
}