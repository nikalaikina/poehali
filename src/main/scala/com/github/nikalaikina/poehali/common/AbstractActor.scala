package com.github.nikalaikina.poehali.common

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, OneForOneStrategy}
import akka.pattern.AskSupport
import com.github.nikalaikina.poehali.util.TimeoutImplicits

import scala.concurrent.ExecutionContext

trait AbstractActor extends Actor with AskSupport with ActorLogging {
  implicit val timeout = TimeoutImplicits.waitForever

  implicit val executionContext = ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newCachedThreadPool()
  )

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e: Exception =>
        e.printStackTrace()
        Restart
    }
}