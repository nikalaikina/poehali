package com.github.nikalaikina.poehali.util

import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps


object TimeoutImplicits {
  implicit val waitForever = Timeout(10 minutes)
}
