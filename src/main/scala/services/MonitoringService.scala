package services

import cats.effect.{IO, Resource}
import clients.{InfluxClient, RepeaterMonitorClient}
import model.*
import model.controller.Commands.{Command, ConfigSet, RTCSet, Telemetry}
import model.controller.ConfigParam.{MainVoltageOff, MainVoltageOn}
import model.controller.{Commands, Outcome, Responses}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.model.Voltages
import utils.Conversions.*

import java.net.*
import java.nio.ByteBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class MonitoringService(telemetryInterval: FiniteDuration, socketClient: RepeaterMonitorClient, influxService: InfluxClient) {
  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  val monitor: IO[Unit] = IO.defer {
    for {
      res <- socketClient.send(Commands.Telemetry())
      _ <- res match
        case Outcome.ACK(Responses.Telemetry(timestamp, panelVoltage, panelCurrent, batteryVoltage, batteryCurrent, globalStatus)) =>
          influxService.saveMonitoring(timestamp, panelVoltage, panelCurrent, batteryVoltage, batteryCurrent, globalStatus)
        case o => logger.error(s"Got unexpected response from monitor: $o")
      _ <- IO.sleep(telemetryInterval)
      _ <- monitor
    } yield ()
  }
}
