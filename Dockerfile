# todo: find lightweight image
FROM openjdk:13-jdk

COPY build/libs/activity-detector.jar /opt/

ENV SPRING_PROFILES_ACTIVE=snapshot
EXPOSE 8080

CMD ["java", "-Xdebug", "-Xrunjdwp:server=y,transport=dt_socket,address=*:8000,suspend=n", "-jar", "/opt/activity-detector.jar"]
