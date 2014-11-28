package com.softwaremill.jvmbot.docker

import akka.actor.Actor
import com.softwaremill.jvmbot.docker.DockerRunner

class CodeRunner extends Actor {
  val Image = "webratio/groovy:2.3.7"
  val runner = new DockerRunner(Image)
  override def receive = {
    case code: String =>
      sender() ! runner.run(code)
  }
}
