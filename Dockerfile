# simple Dockerfile based on a Spring tutorial: https://spring.io/guides/gs/spring-boot-docker/
# modified for automated build on DockerHub

FROM java:8

MAINTAINER benjamin.heasly@gmail.com

## install gradle
RUN apt-get-update \
    && apt-get install -y software-properties-common python-software-properties \
    && add-apt-repository -y ppa:cwchien/gradle \
    && apt-get update \
    && apt-get install -y gradle

## build the app
RUN gradle build \
    && cp build/lib/openhds-rest-0.0.1-SNAPSHOT.jar app.jar

# temp space for Tomcat
VOLUME /tmp

# run the standalone jar (not the war)
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]

# if your DockerHub account is called ninjaben, then run the results like this
# docker run -p 8080:8080 -t ninjaben/openhds-rest
