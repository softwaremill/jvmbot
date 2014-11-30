package com.softwaremill.jvmbot.docker

import scala.annotation.tailrec

object Runners {
  val GroovyImage = "szimano/groovy-eval"
  val ScalaImage = "adamw/scalaeval"
  val NodeImage = "szimano/nashorn-eval"
  val runners = Seq(
    new DockerRunner(GroovyImage, identity),
    new DockerRunner(ScalaImage, identity),
    new DockerRunner(NodeImage, identity)
  )

  def run(code: String): String = {
    run(runners, code)
  }

  @tailrec
  private def run(runners: Seq[DockerRunner], code: String): String = {
    runners match {
      case Nil => "Oops! I couldn't execute that code"
      case x :: xs => x.run(code) match {
        case Some(r) => r
        case None => run(xs, code)
      }
    }
  }
}
