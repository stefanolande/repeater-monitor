package clients.monitor

import io.circe.*
import io.circe.syntax.*
import Responses.Response

object Outcome {
  given Encoder[Outcome] = (a: Outcome) => a.toString.asJson

  def fromBytes(bytes: Array[Byte]): Outcome =
    if (bytes(0) == 'N'.toByte)
      Outcome.NACK
    else
      Response.fromBytes(bytes) match
        case Some(value) => Outcome.ACK(value)
        case None        => Outcome.Invalid
}

enum Outcome {
  case ACK(response: Response)
  case NACK
  case Timeout
  case Invalid

  override def toString: String = this match
    case Outcome.ACK(_)  => "ack"
    case Outcome.NACK    => "nack"
    case Outcome.Timeout => "timeout"
    case Outcome.Invalid => "invalid"
}
