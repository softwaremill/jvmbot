package com.softwaremill.jvmbot

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import com.amazonaws.auth.{PropertiesCredentials, BasicAWSCredentials, AWSCredentials}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.{ScanRequest, AttributeValue, PutItemRequest, BatchGetItemRequest}
import twitter4j._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._

object TwitterClient extends App {
  val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  val mentionConsumer = actorSystem.actorOf(Props(classOf[MentionConsumer]))
  val mentionPuller = actorSystem.actorOf(Props(classOf[MentionPuller], mentionConsumer))
  actorSystem.scheduler.schedule(0.seconds, 1.minute, mentionPuller, Pull)
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

class MentionConsumer extends Actor {
  var consumedMentions: Set[Long] = Set()
  val client = new AmazonDynamoDBClient(new PropertiesCredentials(this.getClass.getResourceAsStream("/aws.properties")))

  val DynamoTable = "jvmbot"
  val DynamoId = "tweet_id"

  override def preStart() = {
    client.setRegion(Region.getRegion(Regions.US_EAST_1))
    val items = client.scan(new ScanRequest(DynamoTable))
    items.getItems.foreach(item => consumedMentions += item.get(DynamoId).getS.toLong)
    println(s"Loaded tweets from dynamo, size: ${consumedMentions.size}")
  }

  override def receive = {
    case s:Status =>
      if (!consumedMentions.contains(s.getId)) {   
        client.putItem(new PutItemRequest(DynamoTable, 
          mapAsJavaMap(Map(DynamoId -> new AttributeValue().withS(s.getId.toString)))))
        println(s"Wrote status with id ${s.getId} to dynamo")
        consumedMentions += s.getId
      }
  }
}

object Pull
