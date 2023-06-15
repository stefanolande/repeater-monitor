package services

import cats.data.OptionT
import cats.effect.{IO, IOApp}
import fs2.{text, Chunk, Stream}
import fs2.io.net.{Network, Socket}
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import com.comcast.ip4s.*
import model.APRSTelemetry
import model.configuration.APRSConfiguration
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import concurrent.duration.DurationInt

object APRSService extends IOApp.Simple {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  override def run: IO[Unit] =
    runClient("IS0EIR", host"rotate.aprs.net", port"14580", "IR0UBN")

  def make(aprsConf: APRSConfiguration) =
    aprsConf.stations.map(station => runClient(aprsConf.connectionCallsign, aprsConf.hostname, aprsConf.port, station.callsign)).parSequence_

  def passcode(callsign: String): Int = {
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

  private def runClient(connectionCallsign: String, host: Hostname, port: Port, stationCallsign: String): IO[Unit] =
    Network[IO]
      .client(SocketAddress(host, port))
      .onFinalize(logger.info(s"Releasing APRS-IS socket for $stationCallsign"))
      .use { socket =>
        validateResponse(socket)
        >> sendLogin(connectionCallsign, stationCallsign, socket)
        >> validateLogin(socket)
        >> readForever(socket) { message =>
          APRSTelemetry.parse(message) match
            case Some(t) => logger.debug(s"Got telemetry: $t".trim)
            case None    => logger.debug(s"Got message: $message".trim)
        }
      }
      .handleErrorWith {
        case LoginFailed => logger.error(s"Login failed, exiting")
        case e =>
          logger.error(s"Error ${e.getMessage}, waiting") >>
          IO.sleep(10.seconds)
          >> runClient(connectionCallsign, host, port, stationCallsign)
      }
}
