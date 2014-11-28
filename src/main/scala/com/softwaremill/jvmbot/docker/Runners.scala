package com.softwaremill.jvmbot.docker

import scala.annotation.tailrec

object Runners {
  val GroovyImage = "szimano/groovy-eval"
  val ScalaImage = "adamw/scalaeval"
  val NodeImage = "szimano/nodejs-eval"
  val runners = Seq(
    new DockerRunner(GroovyImage, (x: String) => s"-e println($x)"),
    new DockerRunner(ScalaImage, (x: String) => s"println($x)"),
    new DockerRunner(NodeImage, (x: String) => s"console.log($x)")
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
