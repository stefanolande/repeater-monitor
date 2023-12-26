package clients

import cats.effect.IO
import cats.effect.kernel.Resource
import com.comcast.ip4s.{Hostname, Port}
import com.influxdb.client.domain.{Bucket, WritePrecision}
import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory, WriteApi}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.Instant

class InfluxClient(influxWriteAPI: WriteApi, arduinoCallsign: String) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def saveAPRS(stationName: String, panelsVoltage: Double, batteryVoltage: Double, aprsPath: String): IO[Unit] =
    for {
      point <- IO(
        Point
          .measurement(stationName)
          .addField("panels-voltage", panelsVoltage)
          .addField("battery-voltage", batteryVoltage)
          .addField("aprs-path", aprsPath)
          .time(Instant.now().toEpochMilli, WritePrecision.MS)
      )
      _ <- logger.debug(s"saving voltages $panelsVoltage and $batteryVoltage to influx")
      _ <- IO.blocking(influxWriteAPI.writePoint(point))
    } yield ()

  def saveMonitoring(timestamp: Int, panelsVoltage: Float, panelsCurrent: Float, batteryVoltage: Float, batteryCurrent: Float): IO[Unit] =
    val point = Point
      .measurement(arduinoCallsign)
      .addField("panels-voltage", panelsVoltage)
      .addField("panels-current", panelsCurrent)
      .addField("battery-voltage", batteryVoltage)
      .addField("battery-current", batteryCurrent)
      .time(timestamp, WritePrecision.S)
    logger.debug(s"[$timestamp] saving panels voltage $panelsVoltage V $panelsCurrent A and battery $batteryVoltage V $batteryCurrent A to influx") >>
    IO.blocking(influxWriteAPI.writePoint(point))

}

object InfluxClient {
  def make(host: Hostname, port: Port, token: String, org: String, bucket: String, arduinoCallsign: String): Resource[IO, InfluxClient] =
    for {
      influxClientFactory <- Resource
        .fromAutoCloseable(IO.blocking(InfluxDBClientFactory.create(s"http://${host.toString}:${port.value}", token.toCharArray, org, bucket)))
      writeApi <- Resource.eval(IO.blocking(influxClientFactory.makeWriteApi()))
    } yield InfluxClient(writeApi, arduinoCallsign)
}
