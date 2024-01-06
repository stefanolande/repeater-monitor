package services

import cats.effect.{IO, Resource}
import clients.influx.InfluxClient
import model.*
import clients.monitor.Commands.{Command, ConfigSet, RTCSet, Telemetry}
import clients.monitor.ConfigParam.{MainVoltageOff, MainVoltageOn}
import clients.monitor.{Commands, Outcome, RepeaterMonitorClient, Responses}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.payloads.Voltages
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
        case Outcome.ACK(Responses.Telemetry(panelVoltage, panelCurrent, batteryVoltage, batteryCurrent, globalStatus)) =>
          influxService.saveMonitoring(panelVoltage, panelCurrent, batteryVoltage, batteryCurrent, globalStatus)
        case o => logger.error(s"Got unexpected response from monitor: $o")
      _ <- IO.sleep(telemetryInterval)
      _ <- monitor
    } yield ()
  }
}
