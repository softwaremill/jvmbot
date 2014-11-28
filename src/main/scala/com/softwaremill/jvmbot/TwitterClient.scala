package com.softwaremill.jvmbot

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.util.Timeout
import com.amazonaws.auth.PropertiesCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.{ScanRequest, AttributeValue, PutItemRequest}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.Message
import com.softwaremill.jvmbot.docker.CodeRunner
import twitter4j._
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.pickling._
import scala.pickling.json._

object TwitterClient extends App {
  val actorSystem = ActorSystem()

  import actorSystem.dispatcher

  val queueSender = actorSystem.actorOf(Props(new MentionQueueSender()))
  val mentionConsumer = actorSystem.actorOf(Props(new MentionConsumer(queueSender)))
  val mentionPuller = actorSystem.actorOf(Props(new MentionPuller(mentionConsumer)))
  val codeRunner = actorSystem.actorOf(Props(new CodeRunner))
  val replySender = actorSystem.actorOf(Props(new ReplySender))
  val queuePuller = actorSystem.actorOf(Props(new MentionQueueReceiver(codeRunner, replySender)))
  actorSystem.scheduler.schedule(0.seconds, 1.minute, mentionPuller, Pull)
  actorSystem.scheduler.schedule(0.seconds, 5.seconds, queuePuller, Pull)
}

class MentionPuller(consumer: ActorRef) extends Actor {
  val twitter = TwitterFactory.getSingleton

  override def receive = {
    case Pull =>
      val statuses = twitter.getMentionsTimeline
      for (status <- statuses) {
        consumer ! status
      }
  }
}

class MentionConsumer(queueSender: ActorRef) extends Actor {

  import AWS._

  var consumedMentions: Set[Long] = Set()

  val DynamoTable = "jvmbot"
  val DynamoId = "tweet_id"

  override def preStart() = {
    dynamoClient.setRegion(Region.getRegion(Regions.US_EAST_1))
    val items = dynamoClient.scan(new ScanRequest(DynamoTable))
    items.getItems.foreach(item => consumedMentions += item.get(DynamoId).getS.toLong)
    println(s"Loaded tweets from dynamo, size: ${consumedMentions.size}")
  }

  override def receive = {
    case s: Status =>
      if (!consumedMentions.contains(s.getId)) {
        dynamoClient.putItem(new PutItemRequest(DynamoTable,
          mapAsJavaMap(Map(DynamoId -> new AttributeValue().withS(s.getId.toString)))))
        println(s"Wrote status with id ${s.getId} to dynamo")
        consumedMentions += s.getId
        queueSender ! s
      }
  }
}

class MentionQueueSender extends Actor {

  import AWS._

  val MessagePrefix = "@jvmbot "

  def codeFromMessage(msg: String): Option[String] =
    if (msg.startsWith(MessagePrefix)) {
      Some(msg.substring(MessagePrefix.length))
    } else None

  override def receive = {
    case s: Status =>
      codeFromMessage(s.getText).foreach(code =>
        sqsClient.sendMessage(queueUrl, CodeTweet(code, s.getUser.getScreenName).pickle.value))
  }
}

class MentionQueueReceiver(codeRunner: ActorRef, replySender: ActorRef) extends Actor {

  import AWS._
  import akka.pattern.ask
  import context.dispatcher

  override def receive = {
    case Pull =>
      sqsClient.receiveMessage(queueUrl).getMessages.foreach { message: Message =>
        sqsClient.deleteMessage(queueUrl, message.getReceiptHandle)
        val status = message.getBody.unpickle[CodeTweet]
        implicit val timeout = Timeout(10.minutes)
        (codeRunner ? status.code).mapTo[String].map(codeResult => replySender ! CodeTweet(codeResult, status.source))
      }
  }
}

class ReplySender extends Actor {
  override def receive = {
    case t: CodeTweet =>
      val twitter = TwitterFactory.getSingleton
      twitter.updateStatus(s"@${t.source} ${t.code}".take(140))
  }
}

object Pull

object AWS {
  val awsCredentials: PropertiesCredentials = new PropertiesCredentials(this.getClass.getResourceAsStream("/aws.properties"))
  val dynamoClient = new AmazonDynamoDBClient(awsCredentials)
  val sqsClient = new AmazonSQSClient(awsCredentials)
  val QueueName = "jvmbot"
  val queueUrl = sqsClient.createQueue(QueueName).getQueueUrl
}

case class CodeTweet(code: String, source: String)