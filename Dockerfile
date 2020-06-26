FROM gradle:6.3-jdk14 as builder
WORKDIR /project
COPY . /project/
# TODO: why root user??
USER root
RUN gradle build -x test

# todo: find lightweight image
FROM openjdk:14-jdk

COPY --from=builder /project/build/libs/activity-detector.jar /opt/

ENV SPRING_PROFILES_ACTIVE=snapshot
EXPOSE 8080

CMD ["java", "-Xdebug", "-Xrunjdwp:server=y,transport=dt_socket,address=*:8000,suspend=n", "-jar", "/opt/activity-detector.jar"]
