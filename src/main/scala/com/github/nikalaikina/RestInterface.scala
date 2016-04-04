package com.github.nikalaikina

import java.sql.Timestamp
import java.time.LocalDate

import akka.actor._
import akka.util.Timeout
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.httpx.SprayJsonSupport._
import spray.httpx.marshalling.{ToResponseMarshallable, Marshaller}
import spray.json.DefaultJsonProtocol
import spray.routing._

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.json.Format
import play.api.libs.json.JsSuccess
import play.api.libs.json.Writes


import scala.concurrent.duration._
import scala.language.postfixOps

class RestInterface extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging { actor: Actor =>

  case class JsonRoute(routes: List[Flight])

  implicit val timeout = Timeout(10 seconds)
  implicit val formats = DefaultFormats
  implicit val personFormat2 = Json.format[Flight]
  implicit val personFormat = Json.format[JsonRoute]


  var quizzes = Vector[Int]()

  def routes: Route =
    pathPrefix("flights") {
      pathEnd {
        get {
          parameters('homeCities ? "VNO, MSQ",
                     'cities ? "BCN, BUD, BGY, AMS, FCO, PRG",
                     'dateFrom  ? "10/08/2016",
                     'dateTo ? "30/09/2016",
                     'daysFrom ? "8",
                     'daysTo ? "14",
                     'cost ? "200",
                     'citiesCount ? "3")
            { (homeCities ,
               cities,
               dateFrom,
               dateTo,
               daysFrom,
               daysTo,
               cost,
               citiesCount) =>
              val settings = new Settings(homeCities,
                                          cities,
                                          dateFrom,
                                          dateTo,
                                          daysFrom,
                                          daysTo,
                                          cost,
                                          citiesCount)
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