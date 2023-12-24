package services

import cats.effect.{IO, Resource}
import model.*
import model.controller.Commands.{Command, ConfigSet, RTCSet, Telemetry}
import model.controller.ConfigParam.{MainVoltageOff, MainVoltageOn}
import model.controller.{Commands, Outcome, Responses}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import routes.model.Voltages
import socket.SocketClient
import utils.Conversions.*

import java.net.*
import java.nio.ByteBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class MonitoringService(socketClient: SocketClient, influxService: InfluxService) {
  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  val monitor: IO[Unit] = IO.defer {
    for {
      res <- socketClient.send(Commands.Telemetry())
      _ <- res match
        case Outcome.ACK(Responses.Telemetry(timestamp, panelVoltage, panelCurrent, batteryVoltage, batteryCurrent)) =>
          influxService.saveController(timestamp, panelVoltage, panelCurrent, batteryVoltage, batteryCurrent)
        case o => logger.error(s"Got unexpected response from monitor: $o")
      _ <- IO.sleep(1.minute)
      _ <- monitor
    } yield ()
  }
}
