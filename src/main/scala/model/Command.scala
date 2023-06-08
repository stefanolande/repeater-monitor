package model

object Command {
  extension (c: Command) {
    def getCode: Byte = c match
      case Command.SetRTC      => 'S'.toByte
      case Command.SetVoltages => 'V'.toByte
  }
}

enum Command {
  case SetRTC
  case SetVoltages
}
