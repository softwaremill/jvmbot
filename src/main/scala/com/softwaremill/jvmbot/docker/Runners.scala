package com.softwaremill.jvmbot.docker

import scala.annotation.tailrec

object Runners {
  val GroovyImage = "szimano/groovy-eval"
  val ScalaImage = "adamw/scalaeval"
  val JavascriptImage = "szimano/nashorn-eval"
  // make sure the runner IDs are unique!
  val runners = Seq(
    new DockerRunner(JavascriptImage, identity, 'j'),
    new DockerRunner(ScalaImage, identity, 's'),
    new DockerRunner(GroovyImage, identity, 'g')
  )
  val runnersByIds = runners.map(r => r.id -> r).toMap
  val hashTagLookupRegex = s".*#([${runners.map{_.id}.mkString(",")}])".r

  def run(code: String): String = {
    code match {
      case hashTagLookupRegex(runnerId) => run(Seq(runnersByIds(runnerId.charAt(0))), code.dropRight(2))
      case _ => run(runners, code)
    }
  }

  @tailrec
  private def run(runners: Seq[DockerRunner], code: String): String = {
    runners match {
      case Nil => s"Oops! I couldn't execute that code ($code)"
      case x :: xs => x.run(code) match {
        case Some(r) => r
        case None => run(xs, code)
      }
    }
  }
}
