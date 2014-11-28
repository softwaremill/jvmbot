name := "jvmbot"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  // util
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.twitter4j" % "twitter4j-core" % "4.0.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.7",
  "com.amazonaws" % "aws-java-sdk" % "1.9.8"
)
