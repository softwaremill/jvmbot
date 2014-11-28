package com.softwaremill.jvmbot.docker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.LogsParameter
import com.spotify.docker.client.messages.ContainerConfig

class DockerRunner(image: String) {
  def run(code: String) = {
    val command = s"-e println $code"
    val docker = DefaultDockerClient.fromEnv().build()
    docker.pull(image)
    val config = ContainerConfig.builder().image(image)
      .cmd(command).networkDisabled(true).attachStderr(true).attachStdout(true).build()
    val creation = docker.createContainer(config)
    val id = creation.id()
    docker.startContainer(id)
    var executionTime = 0
    while (docker.inspectContainer(id).state().running() && executionTime < 60) {
      Thread.sleep(3000L)
      executionTime += 3
    }
    docker.killContainer(id)
    val logs = docker.logs(id, LogsParameter.STDERR, LogsParameter.STDOUT)
    val log = logs.readFully()
    logs.close()
    docker.removeContainer(id)
    docker.close()
    log
  }
}

object DockerRunner extends App {
  val docker = new DockerRunner("webratio/groovy:2.3.7")
  println(docker.run("(1..10).collect{it.toString()}.join(',')"))
}
