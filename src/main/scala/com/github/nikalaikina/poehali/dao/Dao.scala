package com.github.nikalaikina.poehali.dao

import java.time.LocalDate
import java.time.temporal.ChronoUnit._
import java.util.concurrent.TimeUnit

import com.github.nikalaikina.poehali.model.CityDirection
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class Dao {

  implicit val executionContext = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  val cityDao = new CityDao
  val cacheUpdateDao = new CacheUpdateDao

  def directionsToUpdate(take: Int): Future[Seq[CityDirection]] = {
    for {
      cities <- cityDao.all
      cacheUpdates <- cacheUpdateDao.all
    } yield {
      val now = LocalDate.now()
      val aboutToExpire = cacheUpdates.filter(cu => HOURS.between(cu.lastUpdated, now) > 5)
      val citiesRange = cities.map(c => c.name -> (c.usedAsHome + c.used)).toMap
      aboutToExpire
        .map(_.cities)
        .sortBy(d => - citiesRange(d.from) * citiesRange(d.to))
        .take(take)
    }
  }

  def cacheUpdated(direction: CityDirection) {
    val record = cacheUpdateDao.findByDirection(direction.woDirection)
    record.foreach(_.foreach(cu => cacheUpdateDao.update(cu.updated)))
  }

  def cityUsed(city: String): Unit = {
    cityDao.updateByName(city, c => c.copy(used = c.used + 1))
  }

  def cityUsedAsHome(city: String): Unit = {
    cityDao.updateByName(city, c => c.copy(usedAsHome = c.usedAsHome + 1))
  }
}

class CityDao extends BaseDataAccess[City] {

  override def collectionName: String = "cities"

  def findByName(name: String): Future[Option[City]] = {
    collection.find(equal("name", name)).head.map(fromDoc)
  }

  def updateByName(name: String, f: (City => City)): Unit = {
    findByName(name).foreach(_.foreach(c => update(f(c))))
  }

  override def toDoc(e: City): Document = Document(
    "name" -> e.name,
    "used" -> e.used,
    "usedAsHome" -> e.usedAsHome
  )

  override def fromDoc(d: Document): Option[City] = {
    for {
      id <- d.get("_id").map(_.asObjectId().getValue.toString)
      name <- d.get("name").map(_.asString().getValue)
      used <- d.get("used").map(_.asInt32().getValue)
      usedAsHome <- d.get("usedAsHome").map(_.asInt32().getValue)
    } yield City(name, used, usedAsHome, Some(id))
  }
}

class CacheUpdateDao extends BaseDataAccess[CacheUpdate] {

  def findByDirection(direction: CityDirection): Future[Option[CacheUpdate]] = {
    collection.find(equal("cities", direction.woDirection)).head.map(fromDoc)
  }

  override def collectionName: String = "cities"

  override def toDoc(e: CacheUpdate): Document = Document(
    "city1" -> e.cities.woDirection.from,
    "city2" -> e.cities.woDirection.to,
    "lastUpdated" -> e.lastUpdated.toString
  )

  override def fromDoc(d: Document): Option[CacheUpdate] = {
    for {
      id <- d.get("_id").map(_.asObjectId().getValue.toString)
      city1 <- d.get("city1").map(_.asString().getValue)
      city2 <- d.get("city2").map(_.asString().getValue)
      lastUpdated <- d.get("lastUpdated").map(_.asString().getValue)
    } yield CacheUpdate(CityDirection(city1, city2), LocalDate.parse(lastUpdated), Some(id))
  }
}

case class City(name: String, used: Int, usedAsHome: Int, id: Option[String] = None)
  extends DbModel

case class CacheUpdate(cities: CityDirection, lastUpdated: LocalDate, id: Option[String] = None)
  extends DbModel {
  def updated = copy(lastUpdated = LocalDate.now())
}


trait DbModel { val id: Option[String] }

object Helpers {

  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: (Document) => String = (doc) => doc.toJson
  }

  implicit class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))
    def headResult() = Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))
    def printResults(initial: String = ""): Unit = {
      if (initial.length > 0) print(initial)
      results().foreach(res => println(converter(res)))
    }
    def printHeadResult(initial: String = ""): Unit = println(s"${initial}${converter(headResult())}")
  }

}