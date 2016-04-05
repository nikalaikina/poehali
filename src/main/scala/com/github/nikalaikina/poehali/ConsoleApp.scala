package com.github.nikalaikina.poehali

import java.io.{File, PrintWriter}

import com.github.nikalaikina.poehali.api.Settings
import com.github.nikalaikina.poehali.logic.Logic
import com.typesafe.config.{Config, ConfigFactory}

object ConsoleApp extends App {
  val settingsFileName = "settings.properties"
  val outputFilename = "output.txt"

  if (args.isEmpty) {
    Console.err.println("You didn't passed path to settings")
    sys.exit(1)
  }
  val configPath = args(0)
  val config: Config = ConfigFactory.parseFileAnySyntax(new File(configPath + File.separator + settingsFileName))
  val settings = try {
     new Settings(config)
  } catch {
    case e => Console.err.println(s"Error while parsing your settings file $e")
              sys.exit(1)
  }
  println("Parsed settings")
  val pw = new PrintWriter(new File(configPath + File.separator + outputFilename))
  try {
    new Logic(settings).answer().foreach(r => pw.println(r))
  } finally {
    pw.flush()
    pw.close()
  }
  println("Answer is in " + configPath + File.separator + outputFilename)
}
