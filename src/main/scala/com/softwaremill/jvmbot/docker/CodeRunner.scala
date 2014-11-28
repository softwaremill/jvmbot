package com.softwaremill.jvmbot.docker

import akka.actor.Actor

class CodeRunner extends Actor {
  val Image = "webratio/groovy:2.3.7"
  val runner = new DockerRunner(Image)
  override def receive = {
    case code: String =>
      sender() ! runner.run(code)
  }
}
