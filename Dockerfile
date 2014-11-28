# Scala eval

FROM dockerfile/java:oracle-java8
MAINTAINER Adam Warski, adam@warski.org
USER daemon
ADD target/scala-2.11/jvmbot-assembly-1.0.jar /app/main.jar
ENTRYPOINT [ "java", "-cp", "/app/main.jar", "com.softwaremill.jvmbot.eval.ScalaEval" ]