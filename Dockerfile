FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.1_12_1.9.8_3.3.1 as build
COPY . /repeater-monitor
WORKDIR /repeater-monitor
RUN sbt assembly

FROM eclipse-temurin:21
WORKDIR .
COPY --from=build repeater-monitor/target/scala-3.2.2/repeater-monitor.jar .
EXPOSE 8080
CMD java -jar repeater-monitor.jar