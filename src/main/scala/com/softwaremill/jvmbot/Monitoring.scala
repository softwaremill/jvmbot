package com.softwaremill.jvmbot

import java.text.SimpleDateFormat
import java.util.Date

import akka.actor.{ActorSystem, ActorRef, Actor}
import akka.util.Timeout
import spray.routing.SimpleRoutingApp

class StatsActor extends Actor {
  private var inProgress = 0
  private var lastStarted = new Date(0)

  def receive = {
    case CodeRunStarted =>
      inProgress += 1
      lastStarted = new Date()

    case CodeRunStopped =>
      inProgress -= 1

    case GetCodeRunStats =>
      sender() ! CodeRunStats(inProgress, lastStarted)
  }
}

object CodeRunStarted
object CodeRunStopped
object GetCodeRunStats
case class CodeRunStats(inProgress: Int, lastStarted: Date)

class StatsServer(statsActor: ActorRef)(implicit val actorSystem: ActorSystem) extends SimpleRoutingApp {
  def start(): Unit = {
    import akka.pattern.ask
    import scala.concurrent.duration._
    import actorSystem.dispatcher

    implicit val timeout = Timeout(10.seconds)

    startServer(interface = "localhost", port = 8080) {
      get {
        path("stats") {
          complete {
            (statsActor ? GetCodeRunStats).mapTo[CodeRunStats].map { stats =>
              s"""
                 |{
                 |  "in_progress": ${stats.inProgress},
                 |  "last_started": "${new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(stats.lastStarted)}"
                 |}
               """.stripMargin
            }
          }
        }
      }
    }
  }
}