name := "sp"

version := "1.0"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "dreamvoyage.jar"

mainClass in assembly := Some("com.github.nikalaikina.WebAppMain")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= {
  val akkaVersion = "2.4.6"
  val sprayVersion = "1.3.3"
  val json4sVersion = "3.3.0"
  val scalacacheVersion = "0.9.1"

  Seq(
    "org.apache.httpcomponents" % "httpclient" % "4.5",
    "org.json4s" %% "json4s-jackson" % json4sVersion,
    "org.json4s" %% "json4s-native" % json4sVersion,
    "com.typesafe" % "config" % "1.3.0",
    "com.typesafe.play" %% "play-json" % "2.5.1",
    "com.github.mukel" %% "telegrambot4s" % "v1.2.0",

    "io.spray" %% "spray-client" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.2",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",

    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

    "com.github.cb372" %% "scalacache-core" % scalacacheVersion,
    "com.github.cb372" %% "scalacache-guava" % scalacacheVersion,
    "org.terracotta.bigmemory" % "bigmemory" % "4.0.5",
    "net.sf.ehcache" % "ehcache-ee" % "2.7.5",

    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "org.specs2" %% "specs2" % "2.3.13" % "test"
  )
}

scalacOptions ++= Seq(
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps"
)