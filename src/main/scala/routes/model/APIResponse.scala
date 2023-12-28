package routes.model

import model.monitor.Outcome
import model.monitor.Responses.Response
import cats.implicits.*

object APIResponse {
  extension (s: Outcome) {
    def toResponse: APIResponse = s match
      case Outcome.ACK(response) => APIResponse(s, response.some)
      case _                     => APIResponse(s, None)
  }
}

case class APIResponse(status: Outcome, response: Option[Response] = None)
