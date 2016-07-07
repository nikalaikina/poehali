name := "sp"

version := "1.0"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "dreamvoyage.jar"

mainClass in assembly := Some("com.github.nikalaikina.WebAppMain")

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.3.0"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.3.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.1"

libraryDependencies += "com.github.mukel" %% "telegrambot4s" % "v1.2.0"

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.1"
  Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.2",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",

    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

    "com.github.cb372" %% "scalacache-core" % "0.9.1",
    "com.github.cb372" %% "scalacache-guava" % "0.9.1",

    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.specs2" %% "specs2" % "2.3.13" % "test"
  )
}