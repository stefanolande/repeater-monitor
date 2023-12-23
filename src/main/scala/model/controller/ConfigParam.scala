package model.controller

import cats.implicits.*

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
}
