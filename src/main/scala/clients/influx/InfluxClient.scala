package clients.influx

import cats.effect.IO
import cats.effect.kernel.Resource
import clients.influx.InfluxClient
import com.comcast.ip4s.{Hostname, Port}
import com.influxdb.client.domain.{Bucket, WritePrecision}
import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory, WriteApi}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.{Instant, LocalDateTime, ZoneId}

class InfluxClient(influxWriteAPI: WriteApi, monitorCallsign: String) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def saveAPRS(
      stationName: String,
      panelsVoltage: Double,
      batteryVoltage: Double,
      aprsPath: String,
      maybeTime: Option[Long] = None
  ): IO[Unit] =
    for {
      timestamp <- IO(maybeTime.getOrElse(Instant.now().toEpochMilli))
      point <- IO(
        Point
          .measurement(stationName)
          .addField("panels-voltage", panelsVoltage)
          .addField("battery-voltage", batteryVoltage)
          .addField("aprs-path", aprsPath)
          .time(timestamp, WritePrecision.MS)
      )
      _ <- logger.debug(s"[${Instant
          .ofEpochSecond(timestamp)}] saving voltages $panelsVoltage and $batteryVoltage to influx")
      _ <- IO.blocking(influxWriteAPI.writePoint(point))
    } yield ()

  def saveMonitoring(
      panelsVoltage: Float,
      panelsCurrent: Float,
      batteryVoltage: Float,
      batteryCurrent: Float,
      globalStatus: Boolean
  ): IO[Unit] =
    for {
      now       <- IO(Instant.now())
      timestamp <- IO(now.toEpochMilli)
      point =
        Point
          .measurement(monitorCallsign)
          .addField("panels-voltage", panelsVoltage)
          .addField("panels-current", panelsCurrent)
          .addField("battery-voltage", batteryVoltage)
          .addField("battery-current", batteryCurrent)
          .addField("global-status", globalStatus)
          .time(timestamp, WritePrecision.MS)
      _ <- logger.debug(
        s"[$now] saving panels voltage $panelsVoltage V $panelsCurrent A - battery $batteryVoltage V $batteryCurrent A - global status $globalStatus to influx"
      )
      _ <- IO.blocking(influxWriteAPI.writePoint(point))
    } yield ()

}

object InfluxClient {
  def make(host: Hostname, port: Port, token: String, org: String, bucket: String, monitorCallsign: String): Resource[IO, InfluxClient] = {
    val logger: StructuredLogger[IO] = Slf4jLogger.getLogger
    for {
      influxClientFactory <- Resource
        .fromAutoCloseable(IO.blocking(InfluxDBClientFactory.create(s"http://${host.toString}:${port.value}", token.toCharArray, org, bucket)))
      _        <- Resource.eval(logger.debug(s"connecting to server $host:$port with bucket $bucket"))
      writeApi <- Resource.eval(IO.blocking(influxClientFactory.makeWriteApi()))
    } yield InfluxClient(writeApi, monitorCallsign)
  }
}
