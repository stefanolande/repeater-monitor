package model.aprs

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all.*
import clients.influx.InfluxClient
import model.configuration.Station

import java.time.{Clock, LocalDateTime, ZoneOffset}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import scala.util.Try

case class APRSTelemetry(path: String, sequence: Int, values: NonEmptyList[Double], bits: String, timestamp: Option[LocalDateTime]) {

  def saveIfValid(station: Station, influxClient: InfluxClient): IO[Unit] =
    (for {
      panelsVoltage  <- values.get(station.panelsIndex)
      batteryVoltage <- values.get(station.batteryIndex)
      res = influxClient.saveAPRS(station.callsign, panelsVoltage, batteryVoltage, path, timestamp.map(_.toEpochSecond(ZoneOffset.UTC)))
    } yield res).getOrElse(IO.unit)
}

object APRSTelemetry {
  def parse(string: String): Option[APRSTelemetry] =
    Try {
      string.split(':') match {
        case Array(timestampAndPath, message) if message.startsWith("T#") =>
          val (timestamp, path) = {
            val values = timestampAndPath.split(',')
            if (values(0).contains('>')) (None, timestampAndPath)
            else (LocalDateTime.parse(values(0).trim, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).some, values.drop(1).mkString(","))
          }
          val values = message.split(',')
          if (values.length == 7) {
            val sequence = values.head.split('#')(1).toInt
            val numbers  = values.drop(1).dropRight(1).toList.map(_.toDouble)
            APRSTelemetry(path, sequence, NonEmptyList.fromListUnsafe(numbers), values.last.filter(_ >= ' '), timestamp).some
          } else None
        case _ => None
      }
    }.toOption.flatten
}
