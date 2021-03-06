package com.github.nikalaikina.poehali.api

import java.time.{DayOfWeek, Period}

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.config.UsedCities
import com.github.nikalaikina.poehali.dao.RouteCrud
import com.github.nikalaikina.poehali.logic.ManualCacheHeater.Heat
import com.github.nikalaikina.poehali.logic.{Cities, ManualCacheHeater, TripsCalculator}
import com.github.nikalaikina.poehali.message._
import com.github.nikalaikina.poehali.model.{Airport, Trip}
import com.github.nikalaikina.poehali.to.JsonRoute
import spray.http.{HttpHeaders, MediaTypes, StatusCodes}
import spray.routing
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scalacache.ScalaCache


class RestInterface(val citiesProvider: ActorRef, val spApi: ActorRef)(implicit val citiesCache: ScalaCache[Array[Byte]])
  extends HttpServiceActor with AskSupport with RestApi {

  implicit val system = context.system

  def receive = runRoute(routes)
}

object RestInterface {
  def props(citiesProvider: ActorRef, spApi: ActorRef)(implicit citiesCache: ScalaCache[Array[Byte]]) = {
    Props(new RestInterface(citiesProvider, spApi))
  }
}


trait RestApi extends HttpService { actor: Actor with AskSupport =>

  implicit val ec = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  implicit val system: ActorSystem

  implicit val citiesCache: ScalaCache[Array[Byte]]

  val citiesProvider: ActorRef
  val spApi: ActorRef
  val cacheHeater = ManualCacheHeater.start(spApi)
  var citiesContainer: Cities = _
  val routeCrud = new RouteCrud()

  val AccessControlAllowAll = HttpHeaders.RawHeader(
    "Access-Control-Allow-Origin", "*"
  )
  val AccessControlAllowHeadersAll = HttpHeaders.RawHeader(
    "Access-Control-Allow-Headers", "Content-Type,X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5,  Date, X-Api-Version, X-File-Name"
  )
  val AccessControlAllowMethods = HttpHeaders.RawHeader(
    "AccessControlAllowMethods", "POST, GET, PUT, DELETE, OPTIONS"
    )

  import MediaTypes._
  import com.github.nikalaikina.poehali.util.JsonImplicits._
  import com.github.nikalaikina.poehali.util.TimeoutImplicits.waitForever
  import play.api.libs.json._

  override def preStart(): Unit = {
    (citiesProvider ? GetCities).mapTo[Cities].map(c => citiesContainer = c)
  }

  def routes: Route =
    respondWithHeaders(AccessControlAllowAll, AccessControlAllowHeadersAll, AccessControlAllowMethods) {
      pathPrefix("flights") {
        pathEnd {
          get {
            parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'passengers.as[Int] ? 1) {
              processFlightsRequest
            }
          } ~
          post {
            formField('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int], 'passengers.as[Int] ? 1) {
              processFlightsRequest
            }
          }
        }
      } ~
      pathPrefix("cacheHeating") {
        parameters('cities.as[String]) { cities =>
          pathEnd {
            get {
              val list = cities.split(",").toList
              cacheHeater ! Heat(list, Period.ofMonths(6))
              complete("started")
            }
          }
        }
      } ~
      pathPrefix("cacheHeating") {
        pathEnd {
          get {
            cacheHeater ! Heat(UsedCities.cities.toList, Period.ofMonths(6))
            complete("started")
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
                  .mapTo[List[Airport]]
                  .map { x => ctx.complete(Json.toJson(x).toString) }
              }
            }
          }
        }
      } ~
      pathPrefix("cityNames") {
        pathEnd {
          get {
            respondWithMediaType(`application/json`) { (ctx: RequestContext) =>
              (citiesProvider ? GetCityNames)
                .mapTo[List[String]]
                .map { x => ctx.complete(Json.toJson(x).toString) }
            }
          }
        }
      } ~
      pathPrefix("route") {
        pathEnd {
          post {
            entity(as[String]) { routeString =>
              Json.fromJson[JsonRoute](Json.parse(routeString)) match {
                case JsSuccess(route, path) =>
                  routeCrud.insert(route) match {
                    case Some(id) =>
                      complete {
                        id
                      }
                    case None =>
                      complete(StatusCodes.BadRequest)
                  }
                case x =>
                  println(x)
                  complete(StatusCodes.BadRequest)
              }

            }

          }
        } ~
        get {
          path(Segment) { id =>
            respondWithMediaType(`application/json`) { (ctx: RequestContext) =>
              routeCrud.find(id)
              ctx.complete(Json.toJson(routeCrud.find(id)).toString)
            }
          }
        }
      }
    }

  def processFlightsRequest: (String, String, String, String, Int, Int, Int) => routing.Route = {
    (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, passengers) => {
      respondWithMediaType(`application/json`) { (ctx: RequestContext) =>
        val t0 = System.nanoTime()
        val trip = new Trip(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo, passengers)
        (TripsCalculator.logic(spApi, trip) ? GetRoutees(trip))
          .mapTo[Routes]
          .map(r => r.routes.map(tr => JsonRoute(tr.flights)))
          .map { x => {
            ctx.complete(Json.toJson(x).toString)
            println("Elapsed time: " + (System.nanoTime() - t0) / (1000 * 1000 * 1000) + "s")
          } }
      }
    }
  }
}
