package model.controller

import io.circe.*
import io.circe.syntax.*
import model.controller.Responses.Response

object Outcome {
  given Encoder[Outcome] = (a: Outcome) => a.toString.asJson

  def fromBytes(bytes: Array[Byte]): Outcome = bytes(0) match
    case 'A' =>
      Response.fromBytes(bytes.drop(1)) match
        case Some(value) => Outcome.ACK(value)
        case None        => Outcome.Invalid

    case 'N' => Outcome.NACK
    case _   => Outcome.Invalid
}

enum Outcome {
  case ACK(response: Response)
  case NACK
  case Timeout
  case Invalid
}
