package services

import cats.effect.IO
import cats.effect.kernel.Resource
import com.comcast.ip4s.{Hostname, Port}
import com.influxdb.client.domain.{Bucket, WritePrecision}
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory, WriteApi}
import com.influxdb.client.write.Point
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.Instant

class InfluxService(influxWriteAPI: WriteApi) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def save(stationName: String, panelsVoltage: Double, batteryVoltage: Double): IO[Unit] =
    for {
      point <- IO(
        Point
          .measurement("voltage")
          .addTag("repeater", stationName)
          .addField("panels", panelsVoltage)
          .addField("battery", batteryVoltage)
          .time(Instant.now().toEpochMilli, WritePrecision.MS)
      )
      _ <- logger.debug(s"saving voltages $panelsVoltage and $batteryVoltage to influx")
      _ <- IO(influxWriteAPI.writePoint(point))
    } yield ()
}

object InfluxService {
  def make(host: Hostname, port: Port, token: String, org: String, bucket: String): Resource[IO, InfluxService] =
    for {
      influxClientFactory <- Resource
        .fromAutoCloseable(IO(InfluxDBClientFactory.create(s"http://${host.toString}:${port.value}", token.toCharArray, org, bucket)))
      writeApi <- Resource.eval(IO(influxClientFactory.makeWriteApi()))
    } yield InfluxService(writeApi)
}
