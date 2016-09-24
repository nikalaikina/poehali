package com.github.nikalaikina.poehali.api

import akka.actor.{Actor, ActorContext, ActorRef, ActorSystem, Props}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.logic.{Cities, TripsCalculator}
import com.github.nikalaikina.poehali.message.{GetCities, GetPlaces, GetRoutees, Routes}
import com.github.nikalaikina.poehali.model.{Airport, Trip}
import com.github.nikalaikina.poehali.to.JsonRoute
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes
import spray.routing
import spray.routing.RejectionHandler.Default
import spray.routing._

import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scalacache.ScalaCache
import scalacache.serialization.InMemoryRepr


class RestInterface(val citiesProvider: ActorRef, val spApi: ActorRef)(implicit val citiesCache: ScalaCache[InMemoryRepr])
  extends HttpServiceActor with AskSupport with RestApi {

  implicit val system = context.system

  def receive = runRoute(routes)
}

object RestInterface {
  def props(citiesProvider: ActorRef, spApi: ActorRef)(implicit citiesCache: ScalaCache[InMemoryRepr]) = {
    Props(new RestInterface(citiesProvider, spApi))
  }
}


trait RestApi extends HttpService { actor: Actor with AskSupport =>

  implicit val ec = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  implicit val system: ActorSystem

  implicit val citiesCache: ScalaCache[InMemoryRepr]

  val citiesProvider: ActorRef
  val spApi: ActorRef
  var citiesContainer: Cities = _

  import MediaTypes._
  import com.github.nikalaikina.poehali.util.JsonImplicits._
  import com.github.nikalaikina.poehali.util.TimeoutImplicits.waitForever
  import play.api.libs.json._

  override def preStart(): Unit = {
    (citiesProvider ? GetCities).mapTo[Cities].map(c => citiesContainer = c)
  }

  def routes: Route =
    respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
      pathPrefix("flights") {
        pathEnd {
          get {
            parameters('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int]) {
              processFlightsRequest
            }
          } ~
          post {
            formField('homeCities, 'cities, 'dateFrom, 'dateTo, 'daysFrom.as[Int], 'daysTo.as[Int]) {
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
                  .mapTo[List[Airport]]
                  .map { x => ctx.complete(Json.toJson(x).toString) }
              }
            }
          }
        }
      }
    }

  def processFlightsRequest: (String, String, String, String, Int, Int) => routing.Route = {
    (homeCities, cities, dateFrom, dateTo, daysFrom, daysTo) => {
      respondWithMediaType(`application/json`) { (ctx: RequestContext) =>
        val t0 = System.nanoTime()
        val settings = new Trip(homeCities, cities, dateFrom, dateTo, daysFrom, daysTo)
        (TripsCalculator.logic(spApi, citiesContainer) ? GetRoutees(settings))
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