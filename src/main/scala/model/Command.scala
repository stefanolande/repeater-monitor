package model

import services.Conversions.*

object Command {
  extension (c: Command) {
    def getCode: Byte = c match
      case _: SetRTC      => 'S'.toByte
      case _: SetVoltages => 'V'.toByte
  }
}

sealed trait Command {
  def asBytes: Array[Byte]
}
case class SetRTC(timestamp: Int) extends Command {
  def asBytes: Array[Byte] = timestamp.asBytes.prepended(this.getCode)
}
object SetRTC {
  def now: SetRTC = SetRTC((System.currentTimeMillis / 1000).asInstanceOf[Int])
}
case class SetVoltages(voltages: Voltages) extends Command {
  def asBytes: Array[Byte] = voltages.offVoltage.asBytes.prepended(this.getCode) ++ voltages.onVoltage.asBytes
}
