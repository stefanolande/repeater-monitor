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
import services.APRSHistoryService.parseHtml

import java.time.{Duration, LocalDateTime}

class APRSHistoryService(stations: List[Station], client: Client[IO], influxClient: InfluxClient) {

  def importHistory(callsign: String, maybeStart: Option[LocalDateTime]): IO[Unit] = IO.defer {

    def fetch(callsign: String, start: Long) =
      client.expect[String](s"http://www.findu.com/cgi-bin/raw.cgi?call=$callsign&start=$start&length=2400&time=1")

    stations.find(_.callsign == callsign) match {
      case None => IO.raiseError(new RuntimeException(s"invalid callsign $callsign"))

      case Some(station) =>
        val now = LocalDateTime.now()
        val startHour =
          maybeStart match
            case Some(start) => Duration.between(start, now).toHours
            case None        => 0

        for {
          page <- fetch(station.callsign, startHour)
          aprsStringLines = parseHtml(page)
          aprsList        = aprsStringLines.flatMap(APRSTelemetry.parse)
          _ <-
            if (aprsList.nonEmpty) {
              val saveDataIO = Stream
                .emits(aprsList)
                .parEvalMapUnordered(10)(_.saveIfValid(station, influxClient))
                .compile
                .lastOrError
              val minLocalDate = aprsList.flatMap(_.timestamp).min
              val difference   = Duration.between(minLocalDate, now).toHours
              saveDataIO &> fetch(station.callsign, difference)
            } else IO.unit
        } yield ()
    }

  }
}

object APRSHistoryService {
  def parseHtml(html: String): Seq[String] =
    html.split("<tt>")(1).replaceAll("<.*>".r.regex, "").replace("&gt;", ">").replace("\r", "").split("\n").filterNot(_.isEmpty).toSeq

  def make(stations: List[Station], influxClient: InfluxClient): Resource[IO, APRSHistoryService] =
    EmberClientBuilder.default[IO].build.map(client => APRSHistoryService(stations, client, influxClient))
}
