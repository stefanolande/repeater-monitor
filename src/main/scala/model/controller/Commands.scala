package model.controller

import model.*
import utils.Conversions.*

object Commands {
  sealed trait Command {
    def asBytes: Array[Byte] = Array(this.toCode)

    def toCode: Byte = this match {
      case _: Reset      => 'X'.toByte
      case _: Telemetry  => 't'.toByte
      case _: RTCRead    => 'r'.toByte
      case _: RTCSet     => 'R'.toByte
      case _: ConfigRead => 'c'.toByte
      case _: ConfigSet  => 'C'.toByte
      case _: OutputRead => 'o'.toByte
      case _: OutputSet  => 'O'.toByte
    }
  }

  case class Reset() extends Command

  case class Telemetry() extends Command

  case class RTCRead() extends Command

  case class RTCSet(timestamp: Int) extends Command {
    override def asBytes: Array[Byte] = timestamp.asBytes.prepended(this.toCode)
  }

  object RTCSet {
    def now: RTCSet = RTCSet((System.currentTimeMillis / 1000).asInstanceOf[Int])
  }

  case class ConfigRead() extends Command

  case class ConfigSet(configParam: ConfigParam, value: Float) extends Command {
    override def asBytes: Array[Byte] = Array(configParam.toByte, this.toCode) ++ value.asBytes
  }

  case class OutputRead() extends Command

  case class OutputSet(outputNumber: Int, status: Boolean) extends Command {
    override def asBytes: Array[Byte] = Array(
      outputNumber.toByte,
      (if (status) 0x01
       else 0x00).toByte
    )
  }

}
