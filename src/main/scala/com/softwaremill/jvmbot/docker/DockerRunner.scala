package com.softwaremill.jvmbot.docker

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient.LogsParameter
import com.spotify.docker.client.messages.ContainerConfig

class DockerRunner(image:String) {
  def run(code:String) = {
    val docker = DefaultDockerClient.fromEnv().build()
    docker.pull(image)
    val config = ContainerConfig.builder().image(image)
      .cmd(s"-e println $code").attachStderr(true).attachStdout(true).build()
    val creation = docker.createContainer(config)
    val id = creation.id()
    docker.startContainer(id)
    Thread.sleep(3000L)
    docker.killContainer(id)
    val logs = docker.logs(id, LogsParameter.STDERR, LogsParameter.STDOUT)
    val log = logs.readFully()
    println("end")
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