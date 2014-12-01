package com.softwaremill.jvmbot.docker

import akka.actor.{ActorRef, Actor}
import com.softwaremill.jvmbot.{CodeRunStopped, CodeRunStarted}

class CodeRunner(stats: ActorRef) extends Actor {
  override def receive = {
    case code: String =>
      stats ! CodeRunStarted
      try {
        val result = Runners.run(code)
        sender() ! result
      } finally {
        stats ! CodeRunStopped
      }
  }
}
