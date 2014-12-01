package com.softwaremill.jvmbot

import akka.actor.{ActorSystem, Props}
import akka.routing.SmallestMailboxPool
import com.softwaremill.jvmbot.docker.CodeRunner
import com.typesafe.scalalogging.slf4j.StrictLogging

object JVMBot extends App with StrictLogging {
  implicit val actorSystem = ActorSystem()

  val statsActor = actorSystem.actorOf(Props(new StatsActor))

  val codeRunnersRouter = actorSystem.actorOf(SmallestMailboxPool(5).props(Props(new CodeRunner(statsActor))))

  val replySender = actorSystem.actorOf(Props(new ReplySender))
  val queueReceiver = actorSystem.actorOf(Props(new MentionQueueReceiver(codeRunnersRouter, replySender)))
  val queueSender = actorSystem.actorOf(Props(new MentionQueueSender(queueReceiver)))
  val mentionConsumer = actorSystem.actorOf(Props(new MentionConsumer(queueSender)))
  val mentionPuller = actorSystem.actorOf(Props(new MentionPuller(mentionConsumer)))

  mentionPuller ! Restart

  new StatsServer(statsActor).start()
}