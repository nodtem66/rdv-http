# Dockerfile for rdb-http
# Version 0.1.0
FROM java:8
MAINTAINER Jirawat I. <nodtem66@gmail.com>

# Add standalone RDV-HTTP jar file to /opt/rdv-http
RUN mkdir -p /opt/rdv-http/
COPY target/scala-2.11/rdv-http.jar /opt/rdv-http/

WORKDIR /opt/rdv-http
ENTRYPOINT ["/usr/bin/java", "-jar", "rdv-http.jar"]
