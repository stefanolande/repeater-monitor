FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.9_9_1.9.8_3.3.1 as build
COPY . /repeater-monitor
WORKDIR /repeater-monitor
RUN sbt assembly

FROM eclipse-temurin:17-jre
COPY --from=build repeater-monitor/target/scala-3.2.2/repeater-monitor.jar /app
EXPOSE 8080
WORKDIR /app
VOLUME /app/application.conf
CMD java -Dconfig.file="/app/application.conf" -jar /app/repeater-monitor.jar
