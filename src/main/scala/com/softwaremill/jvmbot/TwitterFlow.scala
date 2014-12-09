package com.softwaremill.jvmbot

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, PutItemRequest, ScanRequest}
import com.typesafe.scalalogging.slf4j.StrictLogging
import twitter4j._

import scala.collection.JavaConversions._
import scala.concurrent.duration._

class MentionPuller(consumer: ActorRef) extends Actor with StrictLogging {
  var ts: TwitterStream = null

  override def receive = {
    case Restart =>
      logger.info("Restarting stream")

      if (ts != null) {
        ts.shutdown()
      }

      ts = new TwitterStreamFactory().getInstance()
      ts.addListener(new UserStreamAdapter {
        override def onException(ex: Exception) {
          logger.error("Exception during twitter streaming", ex)
          self ! Restart
        }
        override def onStatus(status: Status) = consumer ! status
      })
      ts.user()
  }
}

class MentionConsumer(queueSender: ActorRef) extends Actor with StrictLogging {

  import com.softwaremill.jvmbot.AWS._

  var consumedMentions: Set[Long] = Set()

  val DynamoTable = "jvmbot"
  val DynamoId = "tweet_id"
  val DynamoMessage = "tweet_msg"

  override def preStart() = {
    dynamoClient.setRegion(Region.getRegion(Regions.US_EAST_1))
    val items = dynamoClient.scan(new ScanRequest(DynamoTable))
    items.getItems.foreach(item => consumedMentions += item.get(DynamoId).getS.toLong)
    logger.info(s"Loaded tweets from dynamo, size: ${consumedMentions.size}")
  }

  override def receive = {
    case s: Status =>
      if (!consumedMentions.contains(s.getId)) {
        logger.info("consuming mention")
        dynamoClient.putItem(new PutItemRequest(DynamoTable,
          mapAsJavaMap(Map(
            DynamoId -> new AttributeValue().withS(s.getId.toString),
            DynamoMessage -> new AttributeValue().withS(s.getText)
          ))))
        logger.info(s"Wrote status with id ${s.getId} to dynamo")
        consumedMentions += s.getId
        queueSender ! s
      }
  }
}

class MentionQueueSender(queueReceiver: ActorRef) extends Actor with StrictLogging {

  val MessagePrefix = "@jvmbot "

  def codeFromMessage(msg: String): Option[String] =
    if (msg.startsWith(MessagePrefix)) {
      Some(msg.substring(MessagePrefix.length))
    } else None

  override def receive = {
    case s: Status =>
      logger.info(s"sending mention to SQS from ${s.getUser.getScreenName}")
      codeFromMessage(s.getText).foreach(code =>
        queueReceiver ! CodeTweet(code, s.getUser.getScreenName, s.getId))
  }
}

class MentionQueueReceiver(codeRunner: ActorRef, replySender: ActorRef) extends Actor with StrictLogging {

  import akka.pattern.ask
  import context.dispatcher

  override def receive = {
    case status: CodeTweet =>
      logger.info("pulling sqs")
      logger.info(s"pulled mention from SQS from ${status.source}")
      implicit val timeout = Timeout(10.minutes)
      (codeRunner ? status.code).mapTo[String].map(codeResult => replySender ! status.copy(code = codeResult))
  }
}

class ReplySender extends Actor with StrictLogging {
  override def receive = {
    case t: CodeTweet =>
      val reply = s"@${t.source} ${t.code}".take(140)
      logger.info(s"replying to tweet from ${t.source}: $reply")
      val twitter = TwitterFactory.getSingleton
      twitter.updateStatus(new StatusUpdate(reply).inReplyToStatusId(t.originalId))
  }
}

object Restart

object AWS {
  val awsCredentials: PropertiesCredentials = new PropertiesCredentials(this.getClass.getResourceAsStream("/aws.properties"))
  val dynamoClient = new AmazonDynamoDBClient(awsCredentials)
}

case class CodeTweet(code: String, source: String, originalId: Long)