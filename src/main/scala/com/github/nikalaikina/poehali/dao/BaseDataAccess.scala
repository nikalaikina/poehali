package com.github.nikalaikina.poehali.dao

import org.bson.types.ObjectId
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{MongoCollection, _}

import scala.concurrent.{ExecutionContext, Future}


trait BaseDataAccess[T <: DbModel] {

  implicit val executionContext = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  val database: MongoDatabase = MongoClient().getDatabase("poehali")
  val collection: MongoCollection[Document] = database.getCollection(collectionName)

  def collectionName: String

  def all: Future[Seq[T]] = collection.find().map(fromDoc).toFuture().map(_.flatten)

  def insert(e: T): Unit = {
    collection.insertOne(toDoc(e))
  }

  def update(e: T): Unit = {
    collection.updateOne(equal("_id", new ObjectId(e.id.get)), toDoc(e))
  }

  def toDoc(e: T): Document
  def fromDoc(d: Document): Option[T]
}
