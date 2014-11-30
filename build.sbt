name := "jvmbot"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.4",
  "org.scala-lang" % "scala-compiler" % "2.11.4",
  // util
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalatest" %% "scalatest" % "2.1.3" % "test",
  "org.twitter4j" % "twitter4j-core" % "4.0.2",
  "org.twitter4j" % "twitter4j-stream" % "4.0.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.7",
  "com.amazonaws" % "aws-java-sdk" % "1.9.8",
  "com.spotify" % "docker-client" % "2.7.1"
)

lazy val node = project

mainClass in assembly := Some("com.softwaremill.jvmbot.TwitterClient")

assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last == "pom.properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last == "pom.xml" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}