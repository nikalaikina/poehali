name := "sp"

version := "1.0"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "dreamvoyage.jar"

mainClass in assembly := Some("com.github.nikalaikina.WebAppMain")

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5"

libraryDependencies += "org.json4s" % "json4s-jackson_2.11" % "3.3.0"

libraryDependencies += "org.json4s" % "json4s-native_2.11" % "3.3.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.5.1"

libraryDependencies += "com.github.mukel" %% "telegrambot4s" % "v1.2.0"

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.2"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" % "spray-routing_2.11" % "1.3.3",
    "io.spray" %% "spray-json" % "1.3.1",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "org.specs2" %% "specs2" % "2.3.13" % "test"
  )
}