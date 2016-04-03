name := "sp"

version := "1.0"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "dreamvoyage.jar"

mainClass in assembly := Some("com.github.nikalaikina.Main")

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5"

libraryDependencies += "org.json4s" % "json4s-jackson_2.11" % "3.3.0"

libraryDependencies += "org.json4s" % "json4s-native_2.11" % "3.3.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"