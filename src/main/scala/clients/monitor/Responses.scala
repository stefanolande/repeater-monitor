package clients.monitor

import cats.implicits.*
import cats.syntax.apply.*
import utils.Conversions.*

import java.util
import java.util.Arrays

object Responses {
  sealed trait Response
  object Response {
    def fromBytes(b: Array[Byte]): Option[Response] = {
      val (head, tail) = b.splitAt(1)
      head(0) match {
        case 'X'       => Reset().some
        case 't'       => Telemetry.fromBytes(tail)
        case 'r' | 'R' => RTC.fromBytes(tail)
        case 'c' | 'C' => Config.fromBytes(tail)
        case 'o' | 'O' => Output.fromBytes(tail)
        case _         => None
      }
    }
  }

  case class Reset() extends Response

  case class Telemetry(panelVoltage: Float, panelCurrent: Float, batteryVoltage: Float, batteryCurrent: Float, globalStatus: Boolean) extends Response
  object Telemetry {
    def fromBytes(b: Array[Byte]): Option[Telemetry] =
      if (b.length == 17) {
        val panelVoltage   = b.slice(0, 4).asFloat
        val panelCurrent   = b.slice(4, 8).asFloat
        val batteryVoltage = b.slice(8, 12).asFloat
        val batteryCurrent = b.slice(12, 16).asFloat
        val globalStatus   = b(16) != 0

        (panelVoltage, panelCurrent, batteryVoltage, batteryCurrent, globalStatus.some).mapN(Telemetry.apply)
      } else None
  }

  case class RTC(timestamp: Int) extends Response
  object RTC {
    def fromBytes(b: Array[Byte]): Option[RTC] =
      if (b.length > 3)
        b.slice(0, 4).asInt.map(RTC.apply)
      else None
  }

  case class Config(configParam: ConfigParam, value: Float) extends Response
  object Config {
    def fromBytes(b: Array[Byte]): Option[Config] =
      if (b.length > 4) {
        val configParam = ConfigParam.fromByte(b(0))
        val value       = b.slice(1, 5).asFloat
        (configParam, value).mapN(Config.apply)
      } else None

  }

  case class Output(outputNumber: Int, status: Boolean) extends Response
  object Output {
    def fromBytes(b: Array[Byte]): Option[Output] =
      if (b.length >= 2) {
        val number: Int = b(0)
        val status      = b(1) > 0
        Output(number, status).some
      } else None
  }

  case class Meteo(pressure: Float, temperature: Float)

  object Meteo {
    def fromBytes(b: Array[Byte]): Option[Meteo] =
      if (b.length == 8) {
        val pressure    = b.slice(0, 4).asFloat
        val temperature = b.slice(4, 8).asFloat

        (pressure, temperature).mapN(Meteo.apply)
      } else None
  }
}
