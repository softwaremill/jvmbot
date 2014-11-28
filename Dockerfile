# Scala eval

FROM dockerfile/java:oracle-java8
MAINTAINER Adam Warski, adam@warski.org
ADD target/scala-2.11/jvmbot-assembly-1.0.jar /app/main.jar
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENTRYPOINT [ "java", "-Dfile.encoding=UTF-8", "-cp", "/app/main.jar", "com.softwaremill.jvmbot.eval.ScalaEval" ]