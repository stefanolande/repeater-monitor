FROM eclipse-temurin:17-jdk-jammy

WORKDIR .

COPY repeater-monitor.jar /

EXPOSE 8080

CMD java -jar repeater-monitor.jar