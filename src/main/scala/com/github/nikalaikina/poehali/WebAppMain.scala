package com.github.nikalaikina.poehali

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nikalaikina.poehali.api.RestInterface
import spray.can.Http

import scala.concurrent.duration._

object WebAppMain extends App {
  val host = "localhost"
  val port = 8888

  implicit val system = ActorSystem("routes-service")

  val api = system.actorOf(Props(new RestInterface()), "httpInterface")

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
    }
}