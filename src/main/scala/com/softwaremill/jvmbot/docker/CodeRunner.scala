package com.softwaremill.jvmbot.docker

import akka.actor.Actor

import scala.annotation.tailrec

class CodeRunner extends Actor {
  val GroovyImage = "webratio/groovy:2.3.7"
  val runners = Seq(new DockerRunner(GroovyImage))

  override def receive = {
    case code: String =>
      sender() ! run(runners, code)
  }

  @tailrec
  private def run(runners: Seq[DockerRunner], code: String): String = {
    runners match {
      case Nil => "Oops!"
      case x :: xs => x.run(code) match {
        case Some(r) => r
        case None => run(runners.tail, code)
      }
    }
  }
}
