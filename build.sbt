name := "jvmbot"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  // util
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.spotify" % "docker-client" % "2.7.1",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test"
)
