package routes.payloads
import cats.implicits.*
import clients.monitor.ConfigParam as MonitorConfigParam
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

import scala.util.Try

enum ConfigParam {
  case MainVoltageOn
  case MainVoltageOff

  def toModel: MonitorConfigParam = this match
    case ConfigParam.MainVoltageOn  => MonitorConfigParam.MainVoltageOn
    case ConfigParam.MainVoltageOff => MonitorConfigParam.MainVoltageOff
}

object ConfigParam {
  given Decoder[ConfigParam] = (c: HCursor) =>
    Decoder.decodeString(c).flatMap { str =>
      Try(ConfigParam.valueOf(str)).toEither.leftMap { _ =>
        DecodingFailure(s"no enum value matched for $str", List(CursorOp.Field(str)))
      }
    }

}

case class Config(param: ConfigParam, value: Float)
