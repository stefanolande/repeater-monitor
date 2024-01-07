package services

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import clients.influx.InfluxClient
import fs2.Stream
import model.aprs.APRSTelemetry
import model.configuration.Station
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import services.APRSHistoryService.parseHtml

import java.time.{Duration, LocalDateTime}
import scala.util.Try

class APRSHistoryService(stations: List[Station], client: Client[IO], influxClient: InfluxClient) {
  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def importHistory(callsign: String, maybeStart: Option[Long]): IO[Unit] = importHistory(callsign, maybeStart.getOrElse(1L), 0)

  private def importHistory(callsign: String, start: Long, failures: Int = 0): IO[Unit] = IO.defer {

    def fetch(callsign: String, start: Long) =
      client.expect[String](s"http://www.findu.com/cgi-bin/raw.cgi?call=$callsign&start=$start&length=3&time=1")

    stations.find(_.callsign == callsign) match {
      case None => IO.raiseError(new RuntimeException(s"invalid callsign $callsign"))
      case Some(station) =>
        for {
          page <- fetch(station.callsign, start)
          aprsStringLines = parseHtml(page)
          _ <- logger.debug(s"APRS history:\n${aprsStringLines.mkString("\n")}")
          aprsList = aprsStringLines.flatMap(APRSTelemetry.parse)
          saveF =
            if (aprsList.nonEmpty) {
              Stream
                .emits(aprsList)
                .parEvalMapUnordered(10)(_.saveIfValid(station, influxClient))
                .compile
                .lastOrError
            } else IO.unit
          recursionF =
            if (failures < 100) {
              logger.debug(s"[$failures attempt] no data for start=$start\n$page") >>
              importHistory(station.callsign, start + 3, failures + 1)
            } else {
              logger.debug(s"exiting after $failures attempts") >>
              IO.unit
            }
          _ <- saveF &> recursionF
        } yield ()
    }

  }
}

object APRSHistoryService {
  def parseHtml(html: String): Seq[String] =
    Try(html.split("<tt>")(1).replaceAll("<.*>".r.regex, "").replace("&gt;", ">").replace("\r", "").split("\n").filterNot(_.isEmpty).toSeq).toOption
      .getOrElse(Nil)

  def make(stations: List[Station], influxClient: InfluxClient): Resource[IO, APRSHistoryService] =
    EmberClientBuilder.default[IO].build.map(client => APRSHistoryService(stations, client, influxClient))
}
