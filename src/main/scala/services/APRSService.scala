package services

import cats.data.OptionT
import cats.effect.{IO, IOApp, MonadCancelThrow}
import cats.syntax.all.*
import clients.influx.InfluxClient
import com.comcast.ip4s.*
import fs2.io.net.{Network, Socket}
import fs2.{Chunk, Stream, text}
import model.aprs.APRSTelemetry
import model.configuration.{APRSConfiguration, Station}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationInt

object APRSService {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def run(aprsConf: APRSConfiguration, influxService: InfluxClient): IO[Unit] =
    aprsConf.stations.map(station => runClient(aprsConf.connectionCallsign, aprsConf.hostname, aprsConf.port, station, influxService)).parSequence_

  private def passcode(callsign: String): Int = {
    val callsignPrefix = callsign.split('-')(0).toUpperCase()
    val initialCode    = 0x73e2
    val code = callsignPrefix.zipWithIndex.foldLeft(initialCode) { case (acc, (char, i)) =>
      val shift = if (i % 2 == 0) 8 else 0
      acc ^ (char.toInt << shift)
    }
    code & 0x7fff
  }

  private def sendLogin(callsign: String, stationCallsign: String, socket: Socket[IO]): IO[Unit] =
    Stream(s"user $callsign pass ${passcode(callsign)} vers aprs-scala 0.0.1 filter p/$stationCallsign\n")
      .evalTap(v => logger.debug(s"sending $v".trim))
      .through(text.utf8.encode)
      .through(socket.writes)
      .compile
      .drain

  private case object LoginFailed extends RuntimeException("connection failed")

  private def readOne(socket: Socket[IO])(f: String => IO[Unit]): IO[Unit] =
    socket.reads
      .through(text.utf8.decode)
      .take(1)
      .compile
      .lastOrError
      .flatMap(response => f(response))

  private def readForever(socket: Socket[IO])(f: String => IO[Unit]): IO[Nothing] =
    socket.reads
      .through(text.utf8.decode)
      .foreach(response => f(response))
      .compile
      .lastOrError

  private def validateResponse(socket: Socket[IO]): IO[Unit] =
    readOne(socket)(response =>
      if (response.startsWith("#"))
        logger.debug(s"Response: $response".trim)
      else IO.raiseError(new RuntimeException(s"Invalid server response: $response".trim))
    )

  private def validateLogin(socket: Socket[IO]) =
    readOne(socket)(response =>
      if (!response.contains("unverified"))
        logger.info(s"Login OK - $response".trim)
      else IO.raiseError(LoginFailed)
    )

  private def runClient(connectionCallsign: String, host: Hostname, port: Port, station: Station, influxService: InfluxClient): IO[Unit] =
    Network[IO]
      .client(SocketAddress(host, port))
      .onFinalize(logger.info(s"Releasing APRS-IS socket for ${station.callsign}"))
      .use { socket =>
        validateResponse(socket)
        >> sendLogin(connectionCallsign, station.callsign, socket)
        >> validateLogin(socket)
        >> readForever(socket) { message =>
          APRSTelemetry.parse(message) match
            case Some(t) =>
              logger.debug(s"Got telemetry: $t".trim) >>
              (for {
                panelsVoltage  <- t.values.get(station.panelsIndex)
                batteryVoltage <- t.values.get(station.batteryIndex)
                res = influxService.saveAPRS(station.callsign, panelsVoltage, batteryVoltage, t.path)
              } yield res).getOrElse(IO.unit)
            case None => logger.debug(s"Got message: $message".trim)
        }
      }
      .handleErrorWith {
        case LoginFailed => logger.error(s"Login failed, exiting")
        case e =>
          logger.error(s"Error ${e.getMessage}, waiting") >>
          IO.sleep(10.seconds)
          >> runClient(connectionCallsign, host, port, station, influxService)
      }
}
