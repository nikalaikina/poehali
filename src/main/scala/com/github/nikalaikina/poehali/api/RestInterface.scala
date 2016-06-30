package com.github.nikalaikina.poehali.api

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskSupport
import akka.util.Timeout
import com.github.nikalaikina.poehali.logic.{Flight, Logic}
import com.github.nikalaikina.poehali.mesagge.{GetPlaces, GetRoutees, Routes}
import com.github.nikalaikina.poehali.sp._
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class RestInterface(fp: FlightsProvider, cp: ActorRef) extends HttpServiceActor with AskSupport with RestApi {
  implicit val system = context.system
  override def flightsProvider = fp
  override def citiesProvider: ActorRef = cp

  def receive = runRoute(routes)
}

case class JsonRoute(flights: List[Flight])

trait RestApi extends HttpService { actor: Actor with AskSupport =>

  implicit val system: ActorSystem

  def flightsProvider: FlightsProvider

  def citiesProvider: ActorRef

  import com.github.nikalaikina.poehali.util.JsonImplicits._
  import play.api.libs.json._
  implicit val timeout = Timeout(10 seconds)

  def routes: Route =
    pathPrefix("flights") {
      pathEnd {
        get {
          parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'cost.as[Int], 'citiesCount.as[Int])
          { (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount) => (ctx: RequestContext) =>
            val settings = new Settings(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount)
            (logic(settings) ? GetRoutees)
              .mapTo[Routes]
              .map(r => r.routes.map(tr => JsonRoute(tr.flights)))
              .map { x => ctx.complete(Json.toJson(x).toString) }

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
            (citiesProvider ? GetPlaces(number))
              .mapTo[List[City]]
              .map { x => ctx.complete(Json.toJson(x).toString) }
          }
        }
      }
    }


  def logic(settings: Settings) = context.actorOf(Props(classOf[Logic], settings, flightsProvider))
}