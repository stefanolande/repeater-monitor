package clients.monitor

import cats.implicits.*
import io.circe.*
import io.circe.syntax.*

enum ConfigParam {
  case MainVoltageOn
  case MainVoltageOff
}

object ConfigParam {
  extension (s: ConfigParam) {
    def toByte: Byte = s match
      case ConfigParam.MainVoltageOn  => 'O'.toByte
      case ConfigParam.MainVoltageOff => 'o'.toByte
  }

  def fromByte(b: Byte): Option[ConfigParam] = b match {
    case 'O' => ConfigParam.MainVoltageOn.some
    case 'o' => ConfigParam.MainVoltageOff.some
  }

  given Encoder[ConfigParam] = {
    case ConfigParam.MainVoltageOn  => "MainVoltageOn".asJson
    case ConfigParam.MainVoltageOff => "MainVoltageOff".asJson
  }
}
