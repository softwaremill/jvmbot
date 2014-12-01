package com.softwaremill.jvmbot.docker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.LogsParameter
import com.spotify.docker.client.messages.ContainerConfig
import com.typesafe.scalalogging.slf4j.StrictLogging

class DockerRunner(image: String, makeCommand: String => String) extends StrictLogging {
  def run(code: String) = {
    logger.info(s"Got code: $code on runner $image")
    val command = makeCommand(code)
    val docker = DefaultDockerClient.fromEnv().build()
    docker.pull(image)
    val config = ContainerConfig.builder().image(image)
      .cmd(command)
      .networkDisabled(true)
      .memory(525950976L)
      .attachStderr(true)
      .attachStdout(true)
      .build()
    val creation = docker.createContainer(config)
    val id = creation.id()
    docker.startContainer(id)
    var executionTime = 0
    while (docker.inspectContainer(id).state().running() && executionTime < 30) {
      Thread.sleep(3000L)
      executionTime += 3
    }
    val result: Option[String] = if (docker.inspectContainer(id).state().exitCode() == 0) {
      docker.killContainer(id)
      val logs = docker.logs(id, LogsParameter.STDERR, LogsParameter.STDOUT)
      val log = logs.readFully()
      logs.close()
      Some(log)
    } else {
      None
    }
    docker.removeContainer(id)
    docker.close()
    result
  }
}
