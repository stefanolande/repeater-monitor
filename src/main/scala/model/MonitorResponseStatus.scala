package model

import io.circe.*
import io.circe.syntax.*

object MonitorResponseStatus {
  given Encoder[MonitorResponseStatus] = (a: MonitorResponseStatus) => a.toString.asJson

  def fromByte(b: Byte): MonitorResponseStatus = b match
    case 'A' => MonitorResponseStatus.ACK
    case 'N' => MonitorResponseStatus.NACK
    case _   => MonitorResponseStatus.Invalid
}

enum MonitorResponseStatus {
  case ACK
  case NACK
  case Timeout
  case Invalid
}
