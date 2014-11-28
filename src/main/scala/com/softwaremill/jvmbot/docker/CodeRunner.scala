package com.softwaremill.jvmbot.docker

import akka.actor.Actor

class CodeRunner extends Actor {
  override def receive = {
    case code: String =>
      sender() ! Runners.run(code)
  }
}
