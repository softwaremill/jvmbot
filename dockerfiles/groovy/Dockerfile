# Groovy eval

FROM webratio/groovy:2.3.7
MAINTAINER Tomasz Szymanski, tom@softwaremill.com
ADD GroovyEval.groovy /app/GroovyEval.groovy
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENTRYPOINT [ "groovy", "/app/GroovyEval.groovy" ]
