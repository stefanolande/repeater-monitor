FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.3_3.2.2 as build
COPY . /repeater-monitor
WORKDIR /repeater-monitor
RUN sbt assembly

FROM eclipse-temurin:20.0.1_9-jdk
WORKDIR .
COPY --from=build repeater-monitor/target/scala-3.2.2/repeater-monitor.jar .
EXPOSE 8080
CMD java -jar repeater-monitor.jar