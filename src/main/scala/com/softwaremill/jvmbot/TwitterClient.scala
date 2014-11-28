package com.softwaremill.jvmbot

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import com.amazonaws.auth.{PropertiesCredentials, BasicAWSCredentials, AWSCredentials}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.{ScanRequest, AttributeValue, PutItemRequest, BatchGetItemRequest}
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.Message
import twitter4j._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.pickling._
import scala.pickling.json._

object TwitterClient extends App {
  val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  val queueSender = actorSystem.actorOf(Props(classOf[MentionQueueSender]))
  val mentionConsumer = actorSystem.actorOf(Props(classOf[MentionConsumer], queueSender))
  val mentionPuller = actorSystem.actorOf(Props(classOf[MentionPuller], mentionConsumer))
  val queuePuller = actorSystem.actorOf(Props(classOf[MentionQueueReceiver]))
  actorSystem.scheduler.schedule(0.seconds, 1.minute, mentionPuller, Pull)
  actorSystem.scheduler.schedule(0.seconds, 5.seconds, queuePuller, Pull)
}

class MentionPuller(consumer: ActorRef) extends Actor {
  val twitter = TwitterFactory.getSingleton


  override def receive = {
    case Pull =>
      val statuses = twitter.getMentionsTimeline
      System.out.println("Showing home timeline.")
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
    case s:Status =>
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

  override def receive = {
    case s:Status =>
      sqsClient.sendMessage(queueUrl, JsonStatus(s.getText, s.getUser.getScreenName).pickle.value)
  }
}

class MentionQueueReceiver extends Actor {
  import AWS._

  override def receive = {
    case Pull =>
      sqsClient.receiveMessage(queueUrl).getMessages.foreach { message: Message =>
        sqsClient.deleteMessage(queueUrl, message.getReceiptHandle)
        val status = message.getBody.unpickle[JsonStatus]
        println(s"got this awesome status: ${status}")
      }
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

case class JsonStatus(msg: String, source: String)