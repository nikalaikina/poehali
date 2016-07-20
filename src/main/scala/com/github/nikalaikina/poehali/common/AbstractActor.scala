package com.github.nikalaikina.poehali.common

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy}
import akka.pattern.AskSupport
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait AbstractActor extends Actor with AskSupport with ActorLogging {
  implicit val timeout = Timeout(600 seconds)

  implicit val executionContext = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e: Exception =>
        log.error(e.toString)
        Resume
    }
}