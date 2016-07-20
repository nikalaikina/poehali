package com.github.nikalaikina.poehali.api

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskSupport
import akka.util.Timeout
import com.github.nikalaikina.poehali.message.{GetPlaces, GetRoutees, Routes}
import com.github.nikalaikina.poehali.model.{City, Trip}
import com.github.nikalaikina.poehali.to.JsonRoute
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes
import spray.routing
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


class RestInterface(cp: ActorRef) extends HttpServiceActor with AskSupport with RestApi {
  implicit val system = context.system
  override def citiesProvider: ActorRef = cp

  def receive = runRoute(routes)
}


trait RestApi extends HttpService { actor: Actor with AskSupport =>

  implicit val ec = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  implicit val system: ActorSystem

  def citiesProvider: ActorRef

  import MediaTypes._
  import com.github.nikalaikina.poehali.util.JsonImplicits._
  import play.api.libs.json._
  implicit val timeout = Timeout(500 seconds)


  def routes: Route =
    respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {(
      pathPrefix("flights") {
        pathEnd {
          get {
            parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'cost.as[Int], 'citiesCount.as[Int]) {
              processFlightsRequest
            }
          } ~
          post {
            formField('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'cost.as[Int], 'citiesCount.as[Int]) {
              processFlightsRequest
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
            parameters('number.as[Int]) { (number) =>
              respondWithMediaType(`application/json`) { (ctx: RequestContext) =>
                (citiesProvider ? GetPlaces(number))
                  .mapTo[List[City]]
                  .map { x => ctx.complete(Json.toJson(x).toString) }
              }
            }
          }
        }
      })
    }

  def processFlightsRequest: (String, String, String, String, Int, Int, Int, Int) => routing.Route = {
    (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount) => {
      respondWithMediaType(`application/json`) { (ctx: RequestContext) =>
        val settings = new Trip(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, cost, citiesCount)
        (logic(settings) ? GetRoutees)
          .mapTo[Routes]
          .map(r => r.routes.map(tr => JsonRoute(tr.flights)))
          .map { x => ctx.complete(Json.toJson(x).toString) }
      }
    }
  }

  def logic(trip: Trip) = context.actorOf(Props(classOf[TripsCalculator], trip))
}