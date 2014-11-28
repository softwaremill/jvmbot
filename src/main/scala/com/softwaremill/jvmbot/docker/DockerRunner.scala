package com.softwaremill.jvmbot.docker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.LogsParameter
import com.spotify.docker.client.messages.ContainerConfig

object DockerRunner extends App {
  val docker = DefaultDockerClient.fromEnv().build()
  docker.pull("java:7")
  val config = ContainerConfig.builder().image("java:7").cmd("sh", "-c", "while :; do echo dupa; sleep 1; done").attachStderr(true).attachStdout(true).build()
  val creation = docker.createContainer(config)
  val id = creation.id()
  docker.startContainer(id)
  Thread.sleep(60000)
  docker.killContainer(id)
  val logs = docker.logs(id, LogsParameter.STDOUT)
  println(logs.readFully())
  logs.close()
  docker.removeContainer(id)
  docker.close()
}
