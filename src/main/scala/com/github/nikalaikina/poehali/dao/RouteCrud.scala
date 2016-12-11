package com.github.nikalaikina.poehali.dao

import com.github.nikalaikina.poehali.to.JsonRoute
import org.mongodb.scala.Document
import play.api.libs.json.{JsSuccess, Json}


class RouteCrud extends BaseDataAccess[JsonRoute] {

  import com.github.nikalaikina.poehali.util.JsonImplicits._

  override def collectionName: String = "savedRoutes"

  override def toDoc(e: JsonRoute): Document = Document(Json.toJson(e).toString())

  override def fromDoc(d: Document): Option[JsonRoute] = {
    Json.fromJson[JsonRoute](Json.parse(d.toJson)) match {
      case JsSuccess(route, path) =>
        Some(route)
      case x =>
        println(x)
        None
    }
  }
}
